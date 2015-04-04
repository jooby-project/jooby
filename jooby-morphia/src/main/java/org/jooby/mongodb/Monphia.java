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

import javax.inject.Provider;

import org.jooby.Env;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
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
 * db = "mongo://localhost/mydb"
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
 * <h1>morphia callback</h1>
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
 * <h1>datastore callback</h1>
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

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    configure(env, config, binder, (uri, client) -> {
      String db = uri.getDatabase();

      Mapper mapper = new Mapper();

      Morphia morphia = new Morphia(mapper);

      if (this.morphiaCbck != null) {
        this.morphiaCbck.accept(morphia, config);
      }

      Provider<Datastore> ds = () -> {
        Datastore datastore = morphia.createDatastore(client.get(), db);
        if (callback != null) {
          callback.accept(datastore);
        }
        return datastore;
      };

      binder.bind(key(Morphia.class, db)).toInstance(morphia);
      binder.bind(key(GuiceObjectFactory.class, db)).asEagerSingleton();
      binder.bind(key(Datastore.class, db)).toProvider(ds).asEagerSingleton();
    });
  }

}
