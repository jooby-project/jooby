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
package org.jooby.cassandra;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Jooby.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalDateCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalTimeCodec;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.annotations.Accessor;
import com.google.inject.Binder;
import com.typesafe.config.Config;

import javaslang.Function3;
import javaslang.control.Try;

/**
 * <h1>cassandra</h1>
 * <p>
 * <a href="http://cassandra.apache.org">The Apache Cassandra</a> database is the right choice when
 * you need scalability and high availability without compromising performance. Linear scalability
 * and proven fault-tolerance on commodity hardware or cloud infrastructure make it the perfect
 * platform for mission-critical data. Cassandra's support for replicating across multiple
 * datacenters is best-in-class, providing lower latency for your users and the peace of mind of
 * knowing that you can survive regional outages.
 * </p>
 *
 * <p>
 * This module offers <a href="http://cassandra.apache.org">cassandra</a> database features via
 * <a href="http://datastax.github.io/java-driver">Datastax Java Driver</a>.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>Cluster</li>
 * <li>Session</li>
 * <li>MappingManager</li>
 * <li>Datastore</li>
 * <li>Optionally a Cassandra {@link org.jooby.Session.Store}</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <p>
 * Via connection string:
 * </p>
 * <pre>{@code
 * {
 *   use(new Cassandra("cassandra://localhost/db"));
 * }
 * }</pre>
 *
 * <p>
 * Via connection property:
 * </p>
 * <pre>{@code
 * {
 *   use(new Cassandra("db"));
 * }
 * }</pre>
 *
 * <p>
 * After you install the module {@link Session}, {@link MappingManager} and {@link Datastore} are
 * ready to use.
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Cassandra("cassandra://localhost/db"));
 *
 *   get("/doWithSession", req -> {
 *     Session session = require(Session.class);
 *     // work with session
 *   });
 *
 *   get("/doWithMappingManager", req -> {
 *     MappingManager manager = require(MappingManager.class);
 *     Mapper<Beer> mapper = manager.mapper(Beer.class);
 *     // work with mapper;
 *   });
 *
 *   get("/doWithDatastore", req -> {
 *     Datastore ds = require(Datastore.class);
 *     // work with datastore;
 *   });
 * }
 * }</pre>
 *
 * <h2>basic crud</h2>
 * <p>
 * This module exports {@link MappingManager} so you are free to use a {@link Mapper}. Jooby also
 * offers the {@link Datastore} service which basically wrap a {@link Mapper} and provides
 * query/read operations.
 * </p>
 * <p>
 * The main advantage of {@link Datastore} over {@link Mapper} is that you require just once
 * instance
 * regardless of your number of entities, but also it provides some useful <code>query*</code>
 * methods.
 * </p>
 *
 * <p>
 * Here is a basic API on top of {@link Datastore}:
 * </p>
 *
 * <pre>{@code
 * {
 *
 *   use("/api/beer")
 *     .post(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       Beer beer = req.body().to(Beer.class);
 *       ds.save(beer);
 *       return beer;
 *     })
 *     .get("/:id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       Beer beer = ds.get(Beer.class, req.param("id").value());
 *       return beer;
 *     })
 *     .get(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       return ds.query(Beer.class, "select * from beer").all();
 *     })
 *     .delete("/:id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       ds.delete(Beer.class, req.param("id").value());
 *       return Results.noContent();
 *     });
 * }
 * }</pre>
 *
 * <p>
 * Keep in mind your entities must be mapped as usual or as required by {@link Mapper}. A great
 * example is available
 * <a href="http://datastax.github.io/java-driver/manual/object_mapper/creating">here</a>
 * </p>
 *
 * <h2>accessors</h2>
 * <p>
 * Accessors provide a way to map custom queries not supported by the default entity mappers.
 * Accessors are created at application startup time via {@link #accesor(Class)} method:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Cassandra("cassandra://localhost/db")
 *    .accessor(UserAccessor.class)
 *   );
 *
 *   get("/users", req -> {
 *     return require(UserAccessor.class).getAll();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The accessor can be required or injected in a MVC route.
 * </p>
 *
 * <h2>dse-driver</h2>
 * <p>
 * Add the <code>dse-driver</code> dependency to your classpath and then:
 * </p>
 *
 * <pre>
 * {
 *    use(new Cassandra(DseCluster::build));
 * }
 * </pre>
 *
 * <p>
 * That's all! Now you can <code>require/inject</code> a <code>DseSession</code>.
 * </p>
 *
 * <h2>async</h2>
 * <p>
 * Async? Of course!!! just use the Datastax async API:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Cassandra("cassandra://localhost/db"));
 *
 *   use("/api/beer")
 *     .post(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       Beer beer = req.body().to(Beer.class);
 *       ds.saveAsync(beer);
 *       return beer;
 *     })
 *     .get("/:id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       ListeneableFuture<Beer> beer = ds.getAsync(Beer.class, req.param("id").value());
 *       return beer;
 *     })
 *     .get(req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       return ds.queryAsync(Beer.class, "select * from beer").all();
 *     })
 *     .delete("/:id", req -> {
 *       Datastore ds = req.require(Datastore.class);
 *       ds.deleteAsync(Beer.class, req.param("id").value());
 *       return Results.noContent();
 *     });
 * }
 * }</pre>
 *
 * <h2>multiple contact points</h2>
 * <p>
 * Multiple contact points are separated by a comma:
 * </p>
 * <pre>{@code
 * {
 *   use(new Casssandra("cassandra://host1,host2/db");
 * }
 * }</pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * Advanced configuration is available via cluster builder callback:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Casssandra("cassandra://localhost/db")
 *     .doWithClusterBuilder(builder -> {
 *       builder.withClusterName("mycluster");
 *     }));
 * }
 * }</pre>
 *
 * <p>
 * Or via cluster callback:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Casssandra("cassandra://localhost/db")
 *     .doWithCluster(cluster -> {
 *       Configuration configuration = cluster.getConfiguration();
 *       // set option
 *     }));
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public class Cassandra implements Module {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final Supplier<Cluster.Builder> BUILDER = Cluster::builder;

  private BiConsumer<Cluster.Builder, Config> ccbuilder;

  private BiConsumer<Cluster, Config> cc;

  private String db;

  @SuppressWarnings("rawtypes")

  private List<Class> accesors = new ArrayList<>();

  private Supplier<Cluster.Builder> builder = Cluster::builder;

  /**
   * Creates a new {@link Cassandra} module.
   *
   * Conenct via connection string:
   *
   * <pre>{@code
   * {
   *   use(new Cassandra("cassandra://localhost/db", DseCluster::builder));
   * }
   * }</pre>
   *
   * Or via property:
   *
   * <pre>{@code
   * {
   *   use(new Cassandra("db", DseCluster::builder));
   * }
   * }</pre>
   *
   * @param db Connection string or a property with a connection String.
   * @param builder Cluster builder.
   */
  public Cassandra(final String db, final Supplier<Cluster.Builder> builder) {
    this.db = requireNonNull(db, "ContactPoint/db key required.");
    this.builder = requireNonNull(builder, "Cluster.Builder required.");
  }

  /**
   * Creates a new {@link Cassandra} module.
   *
   * Conenct via connection string:
   *
   * <pre>{@code
   * {
   *   use(new Cassandra("cassandra://localhost/db"));
   * }
   * }</pre>
   *
   * Or via property:
   *
   * <pre>{@code
   * {
   *   use(new Cassandra("db"));
   * }
   * }</pre>
   *
   * @param db Connection string or a property with a connection String.
   */
  public Cassandra(final String db) {
    this(db, BUILDER);
  }

  /**
   * Creates a new {@link Cassandra} module. A property <code>db</code> property must be present and
   * have a valid connection string.
   */
  public Cassandra(final Supplier<Cluster.Builder> builder) {
    this("db", builder);
  }

  /**
   * Creates a new {@link Cassandra} module. A property <code>db</code> property must be present and
   * have a valid connection string.
   */
  public Cassandra() {
    this("db");
  }

  /**
   * Register an {@link Accessor} which is accessible via require calls or dependency injection.
   *
   * @param accessor An accessor.
   * @return This module.
   */
  public Cassandra accesor(final Class<?> accessor) {
    accesors.add(accessor);
    return this;
  }

  /**
   * Configure a cluster before creating it.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Cassandra doWithClusterBuilder(final BiConsumer<Cluster.Builder, Config> configurer) {
    this.ccbuilder = requireNonNull(configurer, "ClusterBuilder conf callback required.");
    return this;
  }

  /**
   * Configure a cluster before creating it.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Cassandra doWithClusterBuilder(final Consumer<Cluster.Builder> configurer) {
    requireNonNull(configurer, "ClusterBuilder conf callback required.");
    return doWithClusterBuilder((b, c) -> configurer.accept(b));
  }

  /**
   * Configure a cluster after creation.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Cassandra doWithCluster(final BiConsumer<Cluster, Config> configurer) {
    this.cc = requireNonNull(configurer, "Cluster conf callbackrequired.");
    return this;
  }

  /**
   * Configure a cluster after creation.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Cassandra doWithCluster(final Consumer<Cluster> configurer) {
    requireNonNull(configurer, "Cluster conf callbackrequired.");
    return doWithCluster((cc, c) -> configurer.accept(cc));
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    ConnectionString cstr = Try.of(() -> ConnectionString.parse(db))
        .getOrElse(() -> ConnectionString.parse(conf.getString(db)));

    ServiceKey serviceKey = env.serviceKey();

    Function3<Class, String, Object, Void> bind = (type, name, value) -> {
      serviceKey.generate(type, name, k -> {
        binder.bind(k).toInstance(value);
      });
      return null;
    };

    Cluster.Builder builder = this.builder.get()
        .addContactPoints(cstr.contactPoints())
        .withPort(cstr.port());

    // allow user configure cluster builder
    if (ccbuilder != null) {
      ccbuilder.accept(builder, conf);
    }

    log.debug("Starting {}", cstr);

    Cluster cluster = builder.build();

    // allow user configure cluster
    if (cc != null) {
      cc.accept(cluster, conf);
    }

    /** codecs */
    Configuration configuration = cluster.getConfiguration();
    CodecRegistry codecRegistry = configuration.getCodecRegistry();
    // java 8 codecs
    codecRegistry.register(
        InstantCodec.instance,
        LocalDateCodec.instance,
        LocalTimeCodec.instance);

    hierarchy(cluster.getClass(), type -> bind.apply(type, cstr.keyspace(), cluster));

    /** Session + Mapper */
    Session session = cluster.connect(cstr.keyspace());
    hierarchy(session.getClass(), type -> bind.apply(type, cstr.keyspace(), session));

    MappingManager manager = new MappingManager(session);
    bind.apply(MappingManager.class, cstr.keyspace(), manager);
    bind.apply(Datastore.class, cstr.keyspace(), new Datastore(manager));

    /** accessors */
    accesors.forEach(c -> {
      Object accessor = manager.createAccessor(c);
      binder.bind(c).toInstance(accessor);
    });

    env.router()
        .map(new CassandraMapper());

    env.onStop(() -> {
      log.debug("Stopping {}", cstr);
      Try.run(() -> session.close())
          .onFailure(x -> log.error("session.close() resulted in exception", x));

      cluster.close();

      log.info("Stopped {}", cstr);
    });
  }

  @SuppressWarnings("rawtypes")
  static void hierarchy(final Class type, final Consumer<Class> consumer) {
    if (type != Object.class) {
      if (type.getName().startsWith("com.datastax")) {
        consumer.accept(type);
      }
      for (Class i : type.getInterfaces()) {
        hierarchy(i, consumer);
      }
      if (!type.isInterface()) {
        hierarchy(type.getSuperclass(), consumer);
      }
    }
  }

}
