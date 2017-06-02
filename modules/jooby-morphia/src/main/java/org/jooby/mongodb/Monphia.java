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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.internal.mongodb.AutoIncID;
import org.jooby.internal.mongodb.GuiceObjectFactory;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.mapping.Mapper;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * Extends {@link Mongodb} with object-document mapping via {@link Morphia}.
 *
 * Exposes {@link Morphia} and {@link Datastore} services.
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
 *   use(new Monphia());
 *
 *   get("/", req {@literal ->} {
 *     Datastore ds = req.require(Datastore.class);
 *     // work with mydb datastore
 *   });
 * }
 * </pre>
 *
 * <h1>options</h1>
 * <h2>morphia callback</h2>
 * <p>
 * The {@link Morphia} callback let you map classes and/or set mapper options.
 * </p>
 *
 * <pre>
 * {
 *   use(new Monphia()
 *     .doWith((morphia, config) {@literal ->} {
 *       // work with morphia
 *       morphia.map(MyObject.class);
 *     });
 *   );
 * }
 * </pre>
 *
 * For more detailed information, check
 * <a href="https://github.com/mongodb/morphia/wiki/MappingObjects">here</a>
 *
 * <h2>datastore callback</h2>
 * <p>
 * This {@link Datastore} callback is executed only once, it's perfect for checking indexes:
 * </p>
 *
 * <pre>
 * {
 *   use(new Monphia()
 *     .doWith(datastore {@literal ->} {
 *       // work with datastore
 *       datastore.ensureIndexes();
 *       datastore.ensureCap();
 *     });
 *   );
 * }
 * </pre>
 *
 * For more detailed information, check
 * <a href="https://github.com/mongodb/morphia/wiki/Datastore#ensure-indexes-and-caps">here</a>
 *
 * <h2>auto-incremental ID</h2>
 * <p>
 * This modules comes with auto-incremental ID generation.
 * </p>
 * <p>
 * usage:
 * </p>
 * <pre>
 * {
 *   use(new Monphia().with(IdGen.GLOBAL); // or IdGen.LOCAL
 * }
 * </pre>
 * <p>
 * ID must be of type: {@link Long} and annotated with {@link GeneratedValue}:
 * </p>
 * <pre>
 * &#64;Entity
 * public class MyEntity {
 *   &#64;Id &#64;GeneratedValue Long id;
 * }
 * </pre>
 *
 * <p>
 * There two ID gen:
 * </p>
 * <ul>
 * <li>GLOBAL: generates a global and unique ID regardless of entity type.</li>
 * <li>LOCAL: generates an unique ID per entity type.</li>
 * </ul>
 *
 * <h1>entity listeners</h1>
 *
 * <p>
 * Guice will create and inject entity listeners (when need it).
 * </p>
 *
 * <pre>
 * public class MyListener {
 *
 *   private Service service;
 *
 *   &#64;Inject
 *   public MyListener(Service service) {
 *     this.service = service;
 *   }
 *
 *   &#64;PreLoad void preLoad(MyObject object) {
 *     service.doSomething(object);
 *   }
 *
 * }
 * </pre>
 *
 * <p>
 * NOTE: ONLY Constructor injection is supported.
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
public class Monphia extends Mongodb {

  private BiConsumer<Morphia, Config> morphiaCbck;

  private Consumer<Datastore> callback;

  private IdGen gen = null;

  /**
   * Creates a new {@link Monphia} module.
   *
   * @param db Name of the property with the connection URI.
   */
  public Monphia(final String db) {
    super(db);
  }

  /**
   * Creates a new {@link Monphia} using the default property: <code>db</code>.
   */
  public Monphia() {
  }

  /**
   * Morphia startup callback, from here you can map classes, set mapper options, etc..
   *
   * @param callback Morphia callback.
   * @return This module.
   */
  public Monphia doWith(final BiConsumer<Morphia, Config> callback) {
    this.morphiaCbck = requireNonNull(callback, "Mapper callback is required.");
    return this;
  }

  /**
   * {@link Datastore} startup callback, from here you can call {@link Datastore#ensureIndexes()}.
   *
   * @param callback Datastore callback.
   * @return This module.
   */
  public Monphia doWith(final Consumer<Datastore> callback) {
    this.callback = requireNonNull(callback, "Datastore callback is required.");
    return this;
  }

  /**
   * <p>
   * Setup up an {@link EntityInterceptor} on {@link PrePersist} events that generates an
   * incremental ID.
   * </p>
   *
   * Usage:
   * <pre>
   * {
   *   use(new Monphia().with(IdGen.GLOBAL);
   * }
   * </pre>
   *
   * <p>
   * ID must be of type: {@link Long} and annotated with {@link GeneratedValue}:
   * </p>
   * <pre>
   * &#64;Entity
   * public class MyEntity {
   *   &#64;Id &#64;GeneratedValue Long id;
   * }
   * </pre>
   *
   * @param gen an {@link IdGen} strategy
   * @return This module.
   */
  public Monphia with(final IdGen gen) {
    this.gen = requireNonNull(gen, "ID Gen is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    configure(env, conf, binder, (uri, client) -> {
      String db = uri.getDatabase();

      Mapper mapper = new Mapper();

      Morphia morphia = new Morphia(mapper);

      if (this.morphiaCbck != null) {
        this.morphiaCbck.accept(morphia, conf);
      }

      Datastore datastore = morphia.createDatastore(client, mapper, db);
      if (gen != null) {
        mapper.addInterceptor(new AutoIncID(datastore, gen));
      }
      if (callback != null) {
        callback.accept(datastore);
      }

      ServiceKey serviceKey = env.serviceKey();
      serviceKey.generate(Morphia.class, db,
          k -> binder.bind(k).toInstance(morphia));
      serviceKey.generate(Datastore.class, db,
          k -> binder.bind(k).toInstance(datastore));

      env.onStart(registry -> new GuiceObjectFactory(registry, morphia));
    });
  }

}
