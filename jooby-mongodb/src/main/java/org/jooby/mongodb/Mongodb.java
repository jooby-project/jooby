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
package org.jooby.mongodb;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.inject.Named;
import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Exposes {@link MongoClient} and a {@link DB}.
 *
 * <h1>usage</h1>
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * db = "mongodb://localhost/mydb"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Mongodb());
 *
 *   get("/", req {@literal ->} {
 *     MongoClient client = req.require(MongoClient.class);
 *     // work with client
 *     DB = req.require(DB.class);
 *     // work with mydb
 *   });
 * }
 * </pre>
 *
 * Default URI connection property is <code>db</code> but of course you can use any other name:
 *
 * <p>
 * application.conf:
 * </p>
 *
 * <pre>
 * mydb = "mongodb://localhost/mydb"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Mongodb("mydb"));
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
 * mongodb.connectionsPerHost  = 100
 * </pre>
 *
 * <p>
 * or programmatically:
 * </p>
 *
 * <pre>
 * {
 *   use(new Mongodb()
 *     .options((options, config) {@literal ->} {
 *       options.connectionsPerHost(100);
 *     })
 *   );
 * }
 * </pre>
 *
 * <h2>connection URI</h2>
 * <p>
 * Default connection URI is defined by the <code>db</code> property. Mongodb URI looks like:
 * </p>
 *
 * <pre>
 *   mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
 * </pre>
 *
 * For more detailed information please check: {@link MongoClientURI}.
 *
 * <h1>two or more connections</h1>
 * <p>
 * Use {@link #named()} when you need two or more <code>mongodb</code> connections:
 * </p>
 *
 * <pre>
 * db1 = "mongodb://localhost/mydb1"
 * db2 = "mongodb://localhost/mydb2"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Mongodb("db1").named());
 *   use(new Mongodb("db2").named());
 *
 *   get("/", req {@literal ->} {
 *     MongoClient client1 = req.require("mydb1", MongoClient.class);
 *     // work with mydb1
 *     MongoClient client2 = req.require("mydb2", MongoClient.class);
 *     // work with mydb1
 *   });
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.5.0
 */
public class Mongodb implements Jooby.Module {

  private final String db;

  private boolean named;

  private BiConsumer<Builder, Config> options;

  /**
   * Creates a new {@link Mongodb} module.
   *
   * @param db Name of the property with the connection URI.
   */
  public Mongodb(final String db) {
    this.db = requireNonNull(db, "A mongo db is required.");
  }

  /**
   * Creates a new {@link Mongodb} using the default property: <code>db</code>.
   */
  public Mongodb() {
    this("db");
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    configure(env, config, binder, (uri, mongo) -> {
    });
  }

  protected void configure(final Env env, final Config config, final Binder binder,
      final BiConsumer<MongoClientURI, Provider<MongoClient>> callback) {
    MongoClientOptions.Builder options = options(mongodb(config));

    if (this.options != null) {
      this.options.accept(options, config);
    }

    MongoClientURI uri = new MongoClientURI(config.getString(db), options);
    MongodbManaged mongodb = new MongodbManaged(uri);
    String database = uri.getDatabase();
    checkArgument(database != null, "Database not found: " + uri);

    binder.bind(key(MongoClientURI.class, database))
      .toInstance(uri);

    binder.bind(key(MongoClient.class, database))
        .toProvider(mongodb)
        .asEagerSingleton();

    Provider<MongoDatabase> dbprovider = () -> mongodb.get().getDatabase(database);
    binder.bind(key(MongoDatabase.class, database))
        .toProvider(dbprovider)
        .asEagerSingleton();

    env.onStart(mongodb::start);
    env.onStop(mongodb::stop);

    callback.accept(uri, mongodb);
  }

  /**
   * Exposes services with {@link Named} annotation.
   *
   * @return This module.
   */
  public Mongodb named() {
    named = true;
    return this;
  }

  /**
   * Set an options callback.
   *
   * <pre>
   * {
   *   use(new Mongodb()
   *     .options((options, config) {@literal ->} {
   *       options.connectionsPerHost(100);
   *     })
   *   );
   * }
   * </pre>
   *
   * @param options Configure callback.
   * @return This module
   */
  public Mongodb options(final BiConsumer<MongoClientOptions.Builder, Config> options) {
    this.options = requireNonNull(options, "Options callback is required.");
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Mongodb.class, "mongodb.conf");
  }

  protected <T> Key<T> key(final Class<T> type, final String db) {
    return named ? Key.get(type, Names.named(db)) : Key.get(type);
  }

  private MongoClientOptions.Builder options(final Config config) {
    MongoClientOptions.Builder builder = MongoClientOptions.builder();

    builder.connectionsPerHost(config.getInt("connectionsPerHost"));
    builder.threadsAllowedToBlockForConnectionMultiplier(
        config.getInt("threadsAllowedToBlockForConnectionMultiplier")
        );
    builder.maxWaitTime((int) config.getDuration("maxWaitTime", TimeUnit.MILLISECONDS));
    builder.connectTimeout((int) config.getDuration("connectTimeout", TimeUnit.MILLISECONDS));
    builder.socketTimeout((int) config.getDuration("socketTimeout", TimeUnit.MILLISECONDS));
    builder.socketKeepAlive(config.getBoolean("socketKeepAlive"));
    builder.cursorFinalizerEnabled(config.getBoolean("cursorFinalizerEnabled"));
    builder.alwaysUseMBeans(config.getBoolean("alwaysUseMBeans"));
    builder.heartbeatFrequency(config.getInt("heartbeatFrequency"));
    builder.minHeartbeatFrequency(config.getInt("minHeartbeatFrequency"));
    builder.heartbeatConnectTimeout(
        (int) config.getDuration("heartbeatConnectTimeout", TimeUnit.MILLISECONDS)
        );
    builder.heartbeatSocketTimeout(
        (int) config.getDuration("heartbeatSocketTimeout", TimeUnit.MILLISECONDS)
        );

    return builder;
  }

  private Config mongodb(final Config config) {
    Config $mongodb = config.getConfig("mongodb");
    if (config.hasPath("mongodb." + db)) {
      $mongodb = config.getConfig("mongodb." + db).withFallback($mongodb);
    }
    return $mongodb;
  }
}
