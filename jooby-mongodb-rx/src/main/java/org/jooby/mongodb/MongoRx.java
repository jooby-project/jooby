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

import static java.util.Objects.requireNonNull;
import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.Route;
import org.jooby.rx.Rx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.ConnectionString;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ClusterType;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.rx.client.AggregateObservable;
import com.mongodb.rx.client.DistinctObservable;
import com.mongodb.rx.client.FindObservable;
import com.mongodb.rx.client.ListCollectionsObservable;
import com.mongodb.rx.client.ListDatabasesObservable;
import com.mongodb.rx.client.MapReduceObservable;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import com.mongodb.rx.client.MongoObservable;
import com.mongodb.rx.client.ObservableAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.Function3;
import javaslang.control.Try;
import rx.Observable;

/**
 * <h1>mongodb-rx</h1>
 * <p>
 * <a href="http://mongodb.github.io/mongo-java-driver-rx/">MongoDB RxJava Driver: </a> provides
 * composable asynchronous and event-based observable sequences for MongoDB.
 * </p>
 *
 * <p>
 * A MongoDB based driver providing support for <a href="http://reactivex.io">ReactiveX (Reactive
 * Extensions)</a> by using the <a href="https://github.com/ReactiveX/RxJava">RxJava library</a>.
 * All database calls return an
 * <a href="http://reactivex.io/documentation/observable.html">Observable</a> allowing for efficient
 * execution, concise code, and functional composition of results.
 * </p>
 *
 * <p>
 * This module depends on {@link Rx} module, please read the {@link Rx} documentation before using
 * this module.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>{@link MongoClient}</li>
 * <li>{@link MongoDatabase} (when mongo connection string has a database)</li>
 * <li>{@link MongoCollection} (when mongo connection string has a collection)</li>
 * <li>{@link Route.Mapper} for mongo observables</li>
 * </ul>
 *
 * <h2>depends on</h2>
 * <ul>
 * <li>{@link Rx rx module}</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 *
 * import org.jooby.mongodb.MongoRx;
 *
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx());
 *
 *   get("/", req -> {
 *     MongoClient client = req.require(MongoClient.class);
 *     // work with client:
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The <code>mongo-rx</code> module connects to <code>mongodb://localhost</code>. You can change the
 * connection string by setting the <code>db</code> property in your
 * <code>application.conf</code> file:
 * </p>
 *
 * <pre>{@code
 * db = "mongodb://localhost/mydb"
 * }</pre>
 *
 * <p>
 * Or at creation time:
 * </p>
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx("mongodb://localhost/mydb"));
 * }
 * }</pre>
 *
 * <p>
 * If your connection string has a database, then you can require a {@link MongoDatabase} object:
 * </p>
 *
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx("mongodb://localhost/mydb"));
 *
 *   get("/", req -> {
 *     MongoDatabase mydb = req.require(MongoDatabase.class);
 *     return mydb.listCollections();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * And if your connection string has a collection:
 * </p>
 *
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx("mongodb://localhost/mydb.mycol"));
 *
 *   get("/", req -> {
 *     MongoCollection mycol = req.require(MongoCollection.class);
 *     return mycol.find();
 *   });
 * }
 * }</pre>
 *
 * <h2>query the collection</h2>
 * <p>
 * The module let you return {@link MongoObservable} directly as route responses:
 * </p>
 *
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx()
 *      .observableAdapter(observable -> observable.observeOn(Scheduler.io())));
 *
 *   get("/pets", req -> {
 *     MongoDatabase db = req.require(MongoDatabase.class);
 *     return db.getCollection("pets")
 *        .find();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Previous example will list all the <code>Pets</code> from a collection. Please note you don't
 * have to deal with {@link MongoObservable}, instead the module converts {@link MongoObservable} to
 * Jooby async semantics.
 * </p>
 *
 * <h2>multiple databases</h2>
 * <p>
 * Multiple databases are supported by adding multiple {@link MongoRx} instances to your
 * application:
 * </p>
 *
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx("db1"));
 *
 *   use(new MongoRx("db2"));
 *
 *   get("/do-with-db1", req -> {
 *     MongoDatabase db1 = req.require("db1", MongoDatabase.class);
 *   });
 *
 *   get("/do-with-db2", req -> {
 *     MongoDatabase db2 = req.require("db2", MongoDatabase.class);
 *   });
 * }
 * }</pre>
 *
 * The keys <code>db1</code> and <code>db2</code> are connection strings in your
 * <code>application.conf</code>:
 *
 * <pre>{@code
 *   db1 = "mongodb://localhost/db1"
 *
 *   db2 = "mongodb://localhost/db2"
 * }</pre>
 *
 * <h2>observable adapter</h2>
 * <p>
 * {@link ObservableAdapter} provides a simple way to adapt all Observables returned by the driver.
 * On such use case might be to use a different Scheduler after returning the results from MongoDB
 * therefore freeing up the connection thread.
 * </p>
 *
 * <pre>{@code
 * {
 *   // required by MongoRx
 *   use(new Rx());
 *
 *   use(new MongoRx().observableAdapter(o -> o.observeOn(Schedulers.io())));
 * }
 * }</pre>
 *
 * <p>
 * Any computations on Observables returned by the {@link MongoDatabase} or {@link MongoCollection}
 * will use the IO scheduler, rather than blocking the MongoDB Connection thread.
 * </p>
 *
 * <p>
 * Please note the {@link #observableAdapter(Function)} works if (and only if) your connection
 * string points to a database. It won't work on <code>mongo://localhost</code> connection string
 * because there is no database in it.
 * </p>
 *
 * <h2>driver options</h2>
 * <p>
 * Driver options are available via
 * <a href="https://docs.mongodb.com/v3.0/reference/connection-string/">connection string</a>.
 * </p>
 *
 * <p>
 * It is also possible to configure specific options:
 * </p>
 *
 * <pre>{@code
 *
 * db = "mongodb://localhost/pets"
 *
 * mongo {
 *   readConcern: default
 *   writeConcern: ACKNOWLEDGED
 *   cluster {
 *     replicaSetName: name
 *     requiredClusterType: REPLICA_SET
 *   }
 *   pool {
 *     maxSize: 100
 *     minSize: 10
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Each option matches a {@link MongoClientSettings} method.
 * </p>
 *
 *
 * @author edgar
 * @since 1.0.0.CR4
 */
public class MongoRx implements Module {

  private static final AtomicInteger instances = new AtomicInteger(0);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private BiConsumer<MongoClientSettings.Builder, Config> configurer;

  private Optional<ObservableAdapter> adapter = Optional.empty();

  private Optional<CodecRegistry> codecRegistry = Optional.empty();

  private String db;

  /**
   * Creates a new {@link MongoRx} module.
   *
   * @param db A connection string or a property key.
   */
  public MongoRx(final String db) {
    this.db = requireNonNull(db, "Connection String/Database key is required.");
  }

  /**
   * Creates a new {@link MongoRx} module that connects to <code>localhost</code> unless you
   * define/override the <code>db</code> property in your <code>application.conf</code> file.
   */
  public MongoRx() {
    this("db");
  }

  /**
   * Allow further configuration on the {@link MongoClientSettings}.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public MongoRx doWith(final BiConsumer<MongoClientSettings.Builder, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer is required.");
    return this;
  }

  /**
   * Allow further configuration on the {@link MongoClientSettings}.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public MongoRx doWith(final Consumer<MongoClientSettings.Builder> configurer) {
    requireNonNull(configurer, "Configurer is required.");
    return doWith((s, c) -> configurer.accept(s));
  }

  /**
   * Set a {@link ObservableAdapter} to the {@link MongoDatabase} created by this module.
   *
   * @param adapter An {@link ObservableAdapter}.
   * @return This module.
   */
  @SuppressWarnings("rawtypes")
  public MongoRx observableAdapter(final Function<Observable, Observable> adapter) {
    this.adapter = toAdapter(requireNonNull(adapter, "Adapter is required."));
    return this;
  }

  /**
   * Set a {@link CodecRegistry} to the {@link MongoDatabase} created by this module.
   *
   * @param codecRegistry A codec registry.
   * @return This module.
   */
  public MongoRx codecRegistry(final CodecRegistry codecRegistry) {
    this.codecRegistry = Optional.of(codecRegistry);
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.empty(MongoRx.class.getName())
        .withValue("db", ConfigValueFactory.fromAnyRef("mongodb://localhost"));
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    /** connection string */
    ConnectionString cstr = Try.of(() -> new ConnectionString(db))
        .getOrElse(() -> new ConnectionString(conf.getString(db)));

    log.debug("Starting {}", cstr);

    boolean first = instances.getAndIncrement() == 0;
    Function3<Class, String, Object, Void> bind = (type, name, value) -> {
      binder.bind(Key.get(type, Names.named(name))).toInstance(value);
      if (first) {
        binder.bind(Key.get(type)).toInstance(value);
      }
      return null;
    };

    /** settings */
    MongoClientSettings.Builder settings = settings(cstr, dbconf(db, conf));
    if (configurer != null) {
      configurer.accept(settings, conf);
    }
    MongoClient client = MongoClients.create(settings.build());
    bind.apply(MongoClient.class, db, client);

    /** bind database */
    Optional.ofNullable(cstr.getDatabase()).ifPresent(dbname -> {
      // observable adapter
      MongoDatabase predb = adapter
          .map(a -> client.getDatabase(dbname).withObservableAdapter(a))
          .orElseGet(() -> client.getDatabase(dbname));
      // codec registry
      MongoDatabase database = codecRegistry
          .map(predb::withCodecRegistry)
          .orElse(predb);

      bind.apply(MongoDatabase.class, dbname, database);

      /** bind collection */
      Optional.ofNullable(cstr.getCollection()).ifPresent(cname -> {
        MongoCollection<Document> collection = database.getCollection(cname);
        bind.apply(MongoCollection.class, cname, collection);
      });
    });

    /** mapper */
    env.routes()
        .map(mapper());

    log.info("Started {}", cstr);

    env.onStop(() -> {
      log.debug("Stopping {}", cstr);
      client.close();
      log.info("Stopped {}", cstr);
    });
  }

  @SuppressWarnings("rawtypes")
  static Route.Mapper mapper() {
    return Route.Mapper.create("mongo-rx", v -> Match(v).of(
        Case(instanceOf(FindObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(ListCollectionsObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(ListDatabasesObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(AggregateObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(DistinctObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(MapReduceObservable.class), m -> m.toObservable().toList()),
        Case(instanceOf(MongoObservable.class), m -> m.toObservable()),
        Case($(), v)));
  }

  static MongoClientSettings.Builder settings(final ConnectionString cstr, final Config conf) {
    MongoClientSettings.Builder settings = MongoClientSettings.builder();

    settings.clusterSettings(cluster(cstr, conf));
    settings.connectionPoolSettings(pool(cstr, conf));
    settings.heartbeatSocketSettings(socket("heartbeat", cstr, conf));

    withStr("readConcern", conf,
        v -> settings.readConcern(
            Match(v.toUpperCase()).option(
                Case("DEFAULT", ReadConcern.DEFAULT),
                Case("LOCAL", ReadConcern.LOCAL),
                Case("MAJORITY", ReadConcern.MAJORITY))
                .getOrElseThrow(() -> new IllegalArgumentException("readConcern=" + v))));

    withStr("readPreference", conf,
        v -> settings.readPreference(ReadPreference.valueOf(v)));

    settings.serverSettings(server(conf));
    settings.socketSettings(socket("socket", cstr, conf));
    settings.sslSettings(ssl(cstr, conf));

    withStr("writeConcern", conf,
        v -> settings.writeConcern(
            Match(v.toUpperCase()).option(
                Case("W1", WriteConcern.W1),
                Case("W2", WriteConcern.W2),
                Case("W3", WriteConcern.W3),
                Case("ACKNOWLEDGED", WriteConcern.ACKNOWLEDGED),
                Case("JOURNALED", WriteConcern.JOURNALED),
                Case("MAJORITY", WriteConcern.MAJORITY))
                .getOrElseThrow(() -> new IllegalArgumentException("writeConcern=" + v))));

    return settings;
  }

  static SslSettings ssl(final ConnectionString cstr, final Config conf) {
    SslSettings.Builder ssl = SslSettings.builder().applyConnectionString(cstr);
    withConf("ssl", conf, c -> {
      withBool("enabled", c, ssl::enabled);
      withBool("invalidHostNameAllowed", c, ssl::invalidHostNameAllowed);
    });
    return ssl.build();
  }

  static ServerSettings server(final Config dbconf) {
    ServerSettings.Builder server = ServerSettings.builder();
    withConf("server", dbconf, c -> {
      withMs("heartbeatFrequency", c,
          s -> server.heartbeatFrequency(s.intValue(), TimeUnit.MILLISECONDS));
      withMs("minHeartbeatFrequency", c,
          s -> server.minHeartbeatFrequency(s.intValue(), TimeUnit.MILLISECONDS));
    });
    return server.build();
  }

  static SocketSettings socket(final String path, final ConnectionString cstr,
      final Config dbconf) {
    SocketSettings.Builder settings = SocketSettings.builder().applyConnectionString(cstr);
    withConf(path, dbconf, c -> {
      withMs("connectTimeout", c,
          s -> settings.connectTimeout(s.intValue(), TimeUnit.MILLISECONDS));
      withBool("keepAlive", c, settings::keepAlive);
      withMs("readTimeout", c,
          s -> settings.readTimeout(s.intValue(), TimeUnit.MILLISECONDS));
      withInt("receiveBufferSize", c, settings::receiveBufferSize);
      withInt("sendBufferSize", c, settings::sendBufferSize);
    });
    return settings.build();
  }

  static ClusterSettings cluster(final ConnectionString cstr, final Config conf) {
    ClusterSettings.Builder cluster = ClusterSettings.builder().applyConnectionString(cstr);
    withConf("cluster", conf, c -> {
      withInt("maxWaitQueueSize", c, cluster::maxWaitQueueSize);
      withStr("replicaSetName", c, cluster::requiredReplicaSetName);
      withStr("requiredClusterType", c,
          v -> cluster.requiredClusterType(ClusterType.valueOf(v.toUpperCase())));
      withMs("serverSelectionTimeout", c,
          s -> cluster.serverSelectionTimeout(s, TimeUnit.MILLISECONDS));
    });
    return cluster.build();
  }

  static ConnectionPoolSettings pool(final ConnectionString cstr, final Config conf) {
    ConnectionPoolSettings.Builder pool = ConnectionPoolSettings.builder()
        .applyConnectionString(cstr);
    withConf("pool", conf, c -> {
      withMs("maintenanceFrequency", c,
          s -> pool.maintenanceFrequency(s, TimeUnit.MILLISECONDS));
      withMs("maintenanceInitialDelay", c,
          s -> pool.maintenanceInitialDelay(s, TimeUnit.MILLISECONDS));
      withMs("maxConnectionIdleTime", c,
          s -> pool.maxConnectionIdleTime(s, TimeUnit.MILLISECONDS));
      withMs("maxConnectionLifeTime", c,
          s -> pool.maxConnectionLifeTime(s, TimeUnit.MILLISECONDS));
      withInt("maxSize", c, pool::maxSize);
      withInt("maxWaitQueueSize", c, pool::maxWaitQueueSize);
      withMs("maxWaitTime", c,
          s -> pool.maxWaitTime(s, TimeUnit.MILLISECONDS));
      withInt("minSize", c, pool::minSize);
    });
    return pool.build();
  }

  static Config dbconf(final String db, final Config conf) {
    Function<String, Config> ifconf = path -> {
      if (Try.of(() -> conf.hasPath(path)).getOrElse(Boolean.FALSE)) {
        return conf.getConfig(path);
      }
      return ConfigFactory.empty();
    };

    // mongdo.db.* < mongo.*
    return ifconf.apply("mongo." + db)
        .withFallback(ifconf.apply("mongo"));
  }

  static <T> void withMs(final String path, final Config conf,
      final Consumer<Long> callback) {
    withPath(path, conf, callback, () -> conf.getDuration(path, TimeUnit.MILLISECONDS));
  }

  static <T> void withInt(final String path, final Config conf,
      final Consumer<Integer> callback) {
    withPath(path, conf, callback, () -> conf.getInt(path));
  }

  static <T> void withStr(final String path, final Config conf,
      final Consumer<String> callback) {
    withPath(path, conf, callback, () -> conf.getString(path));
  }

  static <T> void withBool(final String path, final Config conf,
      final Consumer<Boolean> callback) {
    withPath(path, conf, callback, () -> conf.getBoolean(path));
  }

  static <T> void withConf(final String path, final Config conf,
      final Consumer<Config> callback) {
    withPath(path, conf, callback, () -> conf.getConfig(path));
  }

  static <T> void withPath(final String path, final Config conf, final Consumer<T> callback,
      final Supplier<T> value) {
    if (conf.hasPath(path)) {
      callback.accept(value.get());
    }
  }

  @SuppressWarnings("rawtypes")
  private static Optional<ObservableAdapter> toAdapter(final Function<Observable, Observable> fn) {
    return Optional.of(new ObservableAdapter() {

      @SuppressWarnings("unchecked")
      @Override
      public <T> Observable<T> adapt(final Observable<T> observable) {
        return fn.apply(observable);
      }
    });
  }

}
