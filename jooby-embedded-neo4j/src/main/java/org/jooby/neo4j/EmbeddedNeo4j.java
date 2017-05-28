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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.neo4j;

import com.google.inject.Binder;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Jooby;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.configuration.Settings;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.nonNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Exposes {@link GraphDatabaseService} and {@link GraphAwareRuntime}.
 *
 * <h1>usage</h1>
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * databaseDir = "/tmp"
 * </pre>
 *
 * <pre>
 * {
 *   use(new EmbeddedNeo4j());
 *
 *   get("/", req {@literal ->} {
 *     // work with db
 *     DB = req.require(GraphDatabaseService.class);
 *   });
 * }
 * </pre>
 *
 * Default DatabaseDir property is <code>databaseDir</code> but of course you can use any other name:
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * mydbDir = "bolt://localhost:7687"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Neo4j("mydbDir"));
 *
 *   get("/", req {@literal ->} {
 *     DB mydb = req.require(GraphDatabaseService.class);
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
 * neo4j.dbms.security.allow_csv_import_from_file_urls  = true
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
 *       properties.put(GraphDatabaseSettings.allow_file_urls, config.get("dbms.security.allow_csv_import_from_file_urls"))
 *     })
 *   );
 * }
 * </pre>
 *
 *
 * @author sbcd90
 * @since 1.1.2
 */
public class EmbeddedNeo4j implements Jooby.Module {

  private final String databaseDir;

  private BiConsumer<Properties, Config> properties;

  private final long timeout = 60000L;

  /**
   * Creates a new {@link EmbeddedNeo4j} module.
   *
   * @param databaseDir Database dir location.
   */
  public EmbeddedNeo4j(final String databaseDir) {
    this.databaseDir = requireNonNull(databaseDir, "A database directory is required");
  }

  /**
   * Creates a new {@link EmbeddedNeo4j} using the default property: <code>databaseDir</code>.
   */
  public EmbeddedNeo4j() {
    this("databaseDir");
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Properties properties = options(embeddedneo4j(config));

    if (nonNull(this.properties)) {
      this.properties.accept(properties, config);
    }

    GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
    GraphDatabaseBuilder builder = graphDatabaseFactory
      .newEmbeddedDatabaseBuilder(new File(properties.getProperty("databaseDir")));
    String database = properties.remove("databaseDir").toString();

    properties.forEach((key, value) ->
      builder.setConfig(Settings.setting(key.toString(), Settings.STRING, "default"), value.toString()));

    GraphDatabaseService dbService = builder.newGraphDatabase();
    GraphAwareRuntime graphRuntime = GraphAwareRuntimeFactory.createRuntime(dbService);
    checkArgument(dbService.isAvailable(timeout), "Cannot connect to Database");

    ServiceKey serviceKey = env.serviceKey();
    serviceKey.generate(GraphDatabaseService.class, database, k -> binder.bind(k).toInstance(dbService));
    serviceKey.generate(GraphAwareRuntime.class, database, k -> binder.bind(k).toInstance(graphRuntime));

    env.onStop(dbService::shutdown);
  }

  /**
   * Set a properties callback.
   *
   * <pre>
   * {
   *   use(new EmbeddedNeo4j()
   *     .properties((properties, config) {@literal ->} {
   *       properties.put(GraphDatabaseSettings.allow_file_urls, true);
   *     })
   *   );
   * }
   * </pre>
   *
   * @param properties Configure callback.
   * @return This module
   */
  public EmbeddedNeo4j properties(final BiConsumer<Properties, Config> properties) {
    this.properties = requireNonNull(properties, "properties callback is required");
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(EmbeddedNeo4j.class, "embedded_neo4j.conf");
  }

  private Properties options(final Config config) {
    Properties properties = new Properties();
    properties.putAll(config.entrySet()
      .stream()
      .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().unwrapped()))
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    return properties;
  }

  private Config embeddedneo4j(final Config config) {
    Config $embeddedNeo4j = config.getConfig("neo4j");

    if (config.hasPath("neo4j")) {
      $embeddedNeo4j = config.getConfig("neo4j").withFallback($embeddedNeo4j);
    }
    return $embeddedNeo4j;
  }
}