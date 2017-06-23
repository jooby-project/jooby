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

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.DBType;
import iot.jcypher.database.remote.BoltDBAccess;
import org.jooby.Env;
import org.jooby.Jooby;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Exposes {@link BoltDBAccess}.
 *
 * <h1>usage</h1>
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * db = "bolt://localhost:7687"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Neo4j());
 *
 *   get("/", req {@literal ->} {
 *     // work with db
 *     DB = req.require(BoltDBAccess.class);
 *   });
 * }
 * </pre>
 *
 * Default DB connection info property is <code>db</code> but of course you can use any other name:
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * mydb = "bolt://localhost:7687"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Neo4j("mydb"));
 *
 *   get("/", req {@literal ->} {
 *     DB mydb = req.require(DB.class);
 *     // work with mydb
 *   });
 * }
 * </pre>
 *
 * <h1>options</h1>
 * <p>
 * Options can be set via <code>.conf</code> file:
 * </p>
 *
 * <pre>
 * neo4j.server_root_uri  = "bolt://localhost:7687"
 * </pre>
 *
 * <p>
 * or programmatically:
 * </p>
 *
 * <pre>
 * {
 *   use(new Neo4j()
 *     .properties((properties, config) {@literal ->} {
 *       properties.put(DBProperties.SERVER_ROOT_URI, config.get("server_root_uri"))
 *     })
 *   );
 * }
 * </pre>
 *
 * <p>
 * Default connection URI is defined by the <code>server_root_uri</code> property. Neo4j URI looks like:
 * </p>
 *
 * <pre>
 *   bolt://host1[:port1]
 * </pre>
 *
 * <p>
 *   Credentials have to be passed separately in <code>username</code> & <code>password</code> params.
 * </p>
 *
 * <h1>two or more connections</h1>
 *
 * <pre>
 * db1 = "bolt://localhost:7687"
 * db2 = "bolt://localhost:7688"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Neo4j("db1"));
 *   use(new Neo4j("db2"));
 *
 *   get("/", req {@literal ->} {
 *     BoltDBAccess client1 = req.require("mydb1", BoltDBAccess.class);
 *     // work with mydb1
 *     BoltDBAccess client2 = req.require("mydb2", BoltDBAccess.class);
 *     // work with mydb1
 *   });
 * }
 * </pre>
 *
 * @author sbcd90
 * @since 1.2.0
 */
public class Neo4j implements Jooby.Module {

  private final String db;

  private BiConsumer<Properties, Config> properties;

  /**
   * Creates a new {@link Neo4j} module.
   *
   * @param db Name of the property with the connection info.
   */
  public Neo4j(final String db) {
    this.db = requireNonNull(db, "A neo4j bolt configuration is required");
  }

  /**
   * Creates a new {@link Neo4j} using the default property: <code>db</code>.
   */
  public Neo4j() {
    this("db");
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Properties properties = options(neo4j(config));

    if (nonNull(this.properties)) {
      this.properties.accept(properties, config);
    }

    String dbUser = nonNull(properties.getProperty("username")) ? properties.getProperty("username"): "";
    String dbPassword = nonNull(properties.getProperty("password")) ? properties.getProperty("password"): "";

    BoltDBAccess remoteClient;
    if (dbUser.isEmpty() && dbPassword.isEmpty()) {
      remoteClient = (BoltDBAccess) DBAccessFactory.createDBAccess(DBType.REMOTE, properties);
    } else {
      remoteClient = (BoltDBAccess) DBAccessFactory.createDBAccess(DBType.REMOTE, properties, dbUser, dbPassword);
    }
    checkArgument(remoteClient.getSession() != null,
      "Cannot connect to Database at: " + properties.get(DBProperties.SERVER_ROOT_URI));
    String database = properties.getProperty(DBProperties.SERVER_ROOT_URI);

    Env.ServiceKey serviceKey = env.serviceKey();
    serviceKey.generate(BoltDBAccess.class, database, k -> binder.bind(k).toInstance(remoteClient));

    env.onStop(remoteClient::close);
  }

  /**
   * Set a properties callback.
   *
   * <pre>
   * {
   *   use(new Neo4j()
   *     .properties((properties, config) {@literal ->} {
   *       properties.put(DBProperties.ARRAY_BLOCK_SIZE, "120);
   *     })
   *   );
   * }
   * </pre>
   *
   * @param properties Configure callback.
   * @return This module
   */
  public Neo4j properties(final BiConsumer<Properties, Config> properties) {
    this.properties = requireNonNull(properties, "properties callback is required");
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Neo4j.class, "neo4j.conf");
  }

  private Properties options(final Config config) {
    Properties properties = new Properties();
    properties.putAll(config.entrySet()
      .stream()
      .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().unwrapped()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return properties;
  }

  private Config neo4j(final Config config) {
    Config $neo4j = config.getConfig("neo4j");

    if (config.hasPath("neo4j." + db)) {
      $neo4j = config.getConfig("neo4j." + db).withFallback($neo4j);
    }
    return $neo4j;
  }
}