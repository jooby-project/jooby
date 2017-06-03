/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jooby.neo4j;

import com.graphaware.neo4j.expire.ExpirationModule;
import com.graphaware.neo4j.expire.config.ExpirationConfiguration;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.jooby.Session;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A {@link Session.Store} powered by
 * <a href="https://neo4j.com/">Neo4j</a>.
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *
 *   session(Neo4jSessionStore.class);
 *
 *   get("/", req {@literal ->} {
 *    req.session().set("name", "jooby");
 *   });
 * }
 * </pre>
 *
 * The <code>name</code> attribute and value will be stored in a
 * <a href="https://neo4j.com/">Neo4j</a>.
 *
 * <h2>options</h2>
 *
 * <h3>timeout</h3>
 * <p>
 * By default, a neo4j session will expire after <code>30 minutes</code>. Changing the default
 * timeout is as simple as:
 * </p>
 *
 * <pre>
 * # 8 hours
 * session.timeout = 8h
 *
 * # 15 seconds
 * session.timeout = 15
 *
 * # 120 minutes
 * session.timeout = 120m
 * </pre>
 *
 * <p>
 * It uses GraphAware's Expire library to automatically remove
 * expired sessions.
 * </p>
 *
 * If no timeout is required, use <code>-1</code>.
 *
 * <h3>session label</h3>
 * <p>
 * It's possible to provide the session label using the <code>neo4j.session.label</code>
 * properties.
 * </p>
 *
 * @author sbcd90
 * @since 1.2.0
 */
public class Neo4jSessionStore implements Session.Store {

  private static final char DOT = '.';

  private static final char UDOT = '\uFF0E';

  private static final char DOLLAR = '$';

  private static final char UDOLLAR = '\uFF04';

  protected final String sessionLabel;

  protected final long timeout;

  private static GraphDatabaseService dbService;
  private static GraphAwareRuntime graphRuntime;

  protected final GraphDatabaseService db;
  protected final GraphAwareRuntime graphAwareRuntime;


  public Neo4jSessionStore(Pair<GraphDatabaseService, GraphAwareRuntime> graph, final String sessionLabel,
                           final long timeoutInSeconds) {
    this.db = requireNonNull(graph.getLeft(), "GraphDatabaseService instance is required");
    this.graphAwareRuntime = requireNonNull(graph.getRight(), "GraphAwareRuntime instance is required");
    this.sessionLabel = requireNonNull(sessionLabel, "Label to store sessions is required");
    this.timeout = timeoutInSeconds;
  }

  @Inject
  public Neo4jSessionStore(@Named("databaseDir") String databaseDir,
                           final @Named("neo4j.session.label") String sessionLabel,
                           final @Named("session.timeout") String timeout) {
    this(getOrCreateGraph(databaseDir), sessionLabel, seconds(timeout));
  }

  @Override
  public Session get(final Session.Builder builder) {
    try(Transaction tx = db.beginTx()) {
      return Optional.ofNullable(db.findNode(Label.label(sessionLabel), "_id", builder.sessionId()))
        .map(node -> {
          Map<String, Object> session = new LinkedHashMap<>(node.getAllProperties());

          Long accessedAt = (Long) session.remove("_accessedAt");
          Long createdAt = (Long) session.remove("_createdAt");
          Long savedAt = (Long) session.remove("_savedAt");
          session.remove("_id");
          session.remove("_expire");

          builder
            .accessedAt(accessedAt)
            .createdAt(createdAt)
            .savedAt(savedAt);
          session.forEach((k, v) -> builder.set(decode(k), v.toString()));
          tx.success();
          return builder.build();
        }).orElse(null);
    }
  }

  @Override
  public void save(final Session session) {
    String id = session.id();
    Map<String, String> attributes = session.attributes();

    try(Transaction tx = db.beginTx()) {
      Optional.ofNullable(db.findNode(Label.label(sessionLabel), "_id", id))
        .map(node -> {
          node.setProperty("_accessedAt", session.accessedAt());
          node.setProperty("_createdAt", session.createdAt());
          node.setProperty("_savedAt", session.savedAt());

          attributes.forEach((k ,v) -> node.setProperty(encode(k), v));

          if (!node.hasProperty("_expire")) {
            node.setProperty("_expire", System.currentTimeMillis() + timeout * 1000);
          }

          return node;
        }).orElseGet(() -> {
        Node node = db.createNode(Label.label(sessionLabel));

        node.setProperty("_accessedAt", session.accessedAt());
        node.setProperty("_createdAt", session.createdAt());
        node.setProperty("_savedAt", session.savedAt());

        attributes.forEach((k, v) -> node.setProperty(encode(k), v));
        node.setProperty("_expire", System.currentTimeMillis() + timeout * 1000);

        return node;
      });
      tx.success();
    }
  }

  @Override
  public void create(Session session) {
    save(session);
  }

  @Override
  public void delete(String id) {
    try(Transaction tx = db.beginTx()) {
      db.findNode(Label.label(sessionLabel), "_id", id).delete();
      tx.success();
    }
  }

  private static long seconds(final String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
        .withValue("timeout", ConfigValueFactory.fromAnyRef(value));
      return config.getDuration("timeout", TimeUnit.SECONDS);
    } catch (NullPointerException ex) {
      return 1800L;
    }
  }

  private String encode(final String key) {
    String value = key;
    if (value.charAt(0) == DOLLAR) {
      value = UDOLLAR + value.substring(1);
    }
    return value.replace(DOT, UDOT);
  }

  private String decode(final String key) {
    String value = key;
    if (value.charAt(0) == UDOLLAR) {
      value = DOLLAR + value.substring(1);
    }
    return value.replace(UDOT, DOT);
  }

  private static Pair<GraphDatabaseService, GraphAwareRuntime> getOrCreateGraph(String databaseDir) {
    if (dbService == null && graphRuntime == null) {
      GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(databaseDir));
      dbService = builder.newGraphDatabase();
      graphRuntime = GraphAwareRuntimeFactory.createRuntime(dbService);

      ExpirationConfiguration configuration = ExpirationConfiguration.defaultConfiguration().withNodeExpirationProperty("_expire");
      graphRuntime.registerModule(new ExpirationModule("EXP", dbService, configuration));

      graphRuntime.start();
      graphRuntime.waitUntilStarted();

      return Pair.of(dbService, graphRuntime);
    }
    return Pair.of(dbService, graphRuntime);
  }
}