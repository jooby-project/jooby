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

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrProperty;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.api.pattern.Node;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import org.jooby.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

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
 * It uses <a href="https://github.com/graphaware/neo4j-expire">GraphAware's Expire</a> library to
 * automatically remove expired sessions.
 * </p>
 *
 * <p>
 * For embedded databases you need to configure the expire module, like:
 * </p>
 *
 * <pre>
 * com.graphaware.runtime.enabled = true
 *
 * com.graphaware.module = [{
 *   class: com.graphaware.neo4j.expire.ExpirationModuleBootstrapper
 *   nodeExpirationProperty: _expire
 * }]
 * </pre>
 *
 * <p>
 * The {@link Neo4jSessionStore} uses the <code>_expire</code> to evict sessions.
 * </p>
 *
 * <p>
 * If you connect to a remote server make sure the expire module was installed. More information at
 * <a href="https://github.com/graphaware/neo4j-expire"></a>.
 * </p>
 *
 * <p>
 * If no timeout is required, use <code>-1</code>.
 * </p>
 *
 * <h3>session label</h3>
 * <p>
 * It's possible to provide the session label using the <code>neo4j.session.label</code> properties.
 * </p>
 *
 * @author sbcd90
 * @since 1.2.0
 */
public class Neo4jSessionStore implements Session.Store {

  private final Set<String> SPECIAL = ImmutableSet.of("_accessedAt", "_createdAt", "_savedAt",
      "_expire", "_id");

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String label;

  private final LongSupplier expire;

  private final IDBAccess db;

  public Neo4jSessionStore(final IDBAccess dbaccess, final String sessionLabel,
      final long timeoutInSeconds) {
    this.db = dbaccess;
    this.label = requireNonNull(sessionLabel, "Label to store sessions is required");
    long expire = TimeUnit.SECONDS.toMillis(timeoutInSeconds);
    this.expire = () -> System.currentTimeMillis() + expire;
  }

  @Inject
  public Neo4jSessionStore(final IDBAccess dbaccess,
      final @Named("neo4j.session.label") String sessionLabel,
      final @Named("session.timeout") String timeout) {
    this(dbaccess, sessionLabel, seconds(timeout));
  }

  @Override
  public Session get(final Session.Builder builder) {
    String sid = builder.sessionId();
    JcNode node = new JcNode("n");
    JcQuery query = new JcQuery();
    query.setClauses(new IClause[]{
        MATCH.node(node).label(label).property("_id").value(sid),
        // touch session
        DO.SET(node.property("_expire")).to(expire.getAsLong()),
        RETURN.value(node)
    });

    List<GrNode> result = db.execute(query).resultOf(node);
    log.debug("touch {} session {} ", sid, result);
    if (result.size() == 1) {
      GrNode found = result.get(0);
      builder
          .accessedAt(((Number) found.getProperty("_accessedAt").getValue()).longValue())
          .createdAt(((Number) found.getProperty("_createdAt").getValue()).longValue())
          .savedAt(((Number) found.getProperty("_savedAt").getValue()).longValue());

      found.getProperties()
          .stream()
          .filter(it -> !SPECIAL.contains(it.getName()))
          .forEach(p -> builder.set(p.getName(), p.getValue().toString()));

      return builder.build();
    }

    return null;
  }

  @Override
  public void save(final Session session) {
    String sid = session.id();
    Map<String, Object> attributes = new HashMap<>(session.attributes());

    JcNode node = new JcNode("n");
    JcQuery query = new JcQuery();
    List<IClause> clauses = new ArrayList<>();
    clauses.add(MATCH.node(node).label(label).property("_id").value(sid));
    attributes.put("_accessedAt", session.accessedAt());
    attributes.put("_createdAt", session.createdAt());
    attributes.put("_savedAt", session.savedAt());
    attributes.put("_expire", expire.getAsLong());
    attributes.forEach((k, v) -> clauses.add(DO.SET(node.property(k)).to(v)));
    clauses.add(RETURN.value(node));

    query.setClauses(clauses.toArray(new IClause[clauses.size()]));
    List<GrNode> nodes = db.execute(query).resultOf(node);
    if (nodes.size() == 1) {
      GrNode found = nodes.get(0);
      Set<String> keys = found.getProperties().stream()
          .map(GrProperty::getName)
          .collect(Collectors.toSet());
      keys.removeAll(attributes.keySet());
      // unset properties
      if (keys.size() > 0) {
        log.debug("removing {} => {}", sid, keys);
        JcQuery unsetQuery = new JcQuery();
        List<IClause> unsetClauses = new ArrayList<>();
        unsetClauses.add(MATCH.node(node).label(label).property("_id").value(sid));
        keys.forEach(key -> unsetClauses.add(DO.REMOVE(node.property(key))));
        unsetQuery.setClauses(unsetClauses.toArray(new IClause[unsetClauses.size()]));
        db.execute(unsetQuery);
      }
      log.debug("saved {} => {}", sid, nodes);
    } else {
      // create session:
      query = new JcQuery();
      Node create = CREATE.node(node).label(label);
      create.property("_id").value(sid);
      attributes.forEach((k, v) -> create.property(k).value(v));

      query.setClauses(new IClause[]{create });
      List<GrNode> result = db.execute(query).resultOf(node);
      log.debug("created {} => {}", sid, result);
    }
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    JcNode session = new JcNode("n");
    JcQuery q = new JcQuery();
    q.setClauses(new IClause[]{
        MATCH.node(session).label(label).property("_id").value(id),
        DO.DELETE(session)
    });
    JcQueryResult rsp = db.execute(q);
    log.debug("destroyed {} => {}", id, rsp);
  }

  private static long seconds(final String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
          .withValue("timeout", ConfigValueFactory.fromAnyRef(value));
      return config.getDuration("timeout", TimeUnit.SECONDS);
    }
  }
}
