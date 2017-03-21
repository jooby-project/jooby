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
package org.jooby.requery;

import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Jooby.Module;
import org.jooby.jdbc.Jdbc;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.util.Types;
import com.typesafe.config.Config;

import io.requery.EntityStore;
import io.requery.TransactionListener;
import io.requery.async.CompletableEntityStore;
import io.requery.async.CompletionStageEntityStore;
import io.requery.meta.EntityModel;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveSupport;
import io.requery.reactor.ReactorEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.EntityStateListener;
import io.requery.sql.SchemaModifier;
import io.requery.sql.StatementListener;
import io.requery.sql.TableCreationMode;

/**
 * <h1>requery</h1>
 * <p>
 * Safe, clean and efficient database access via
 * <a href="https://github.com/requery/requery">Requery.</a>
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Jdbc());
 *
 *   use(new Requery(Models.DEFAULT));
 *
 *   get("/people", () -> {
 *     EntityStore store = require(EntityStore.class);
 *     return store.select(Person.class)
 *        .where(Person.ID.eq(req.param("id").intValue()))
 *        .get()
 *        .first();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * This module requires a {@link DataSource} connection. That's why you also need the
 * {@link Jdbc} module.
 * </p>
 *
 * <h2>code generation</h2>
 *
 * <h3>maven</h3>
 * <p>
 * We do provide code generation via Maven profile. All you have to do is to write a
 * <code>requery.activator</code> file inside the <code>src/etc</code> folder. File presence
 * triggers Requery annotation processor and generated contents.
 * </p>
 *
 * <p>
 * Generated content can be found at: <code>target/generated-sources</code>. You can change the
 * default output location by setting the build property <code>requery.output</code> in your
 * <code>pom.xml</code>.
 * </p>
 *
 * <h3>gradle</h3>
 * <p>
 * Please refer to <a href=
 * "https://github.com/requery/requery/wiki/Gradle-&-Annotation-processing#annotation-processing">requery
 * documentation</a> for Gradle.
 * </p>
 *
 * <h2>schema generation</h2>
 *
 * <pre>{@code
 * {
 *   use(new Requery(Models.DEFAULT)
 *     .schema(TableCreationMode.DROP_CREATE)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Optionally, schema generation could be set from .conf file via <code>requery.schema</code>
 * property.
 * </p>
 *
 * <h3>listeners</h3>
 *
 * <pre>{@code
 * public class MyListener implements EntityStateListener<Person> {
 *    &#64;Inject
 *    public MyListener(Dependency dep) {
 *      this.dep = dep;
 *    }
 *
 *    ...
 * }
 *
 * {
 *   use(new Requery(Models.DEFAULT)
 *     .entityStateListener(MyListener.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Support for {@link TransactionListener} and {@link StatementListener} is also provided:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Requery(Models.DEFAULT)
 *     .statementListener(MyStatementListener.class)
 *     .transactionListener(TransactionListener.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * You can add as many listener as you need. Each listener will be created by <code>Guice</code>
 * </p>
 *
 * <h2>Type-Safe injection</h2>
 * <p>
 * If you love <code>DAO</code> like classes, we are happy to tell you that it you easily inject
 * type-safe {@link EntityStore}:
 * </p>
 *
 * <pre>{@code
 *
 * public class PersonDAO {
 *   private EntityStore&lt;Persistable, Person&gt; store;
 *
 *   &#64;Inject
 *   public PersonDAO(EntityStore&lt;<Persistable, Person&gt; store) {
 *     this.store = store;
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Please note we don't inject a <code>raw</code> {@link EntityStore}. Instead we ask for a
 * <code>Person</code> {@link EntityStore}. You can safely inject a {@link EntityStore} per each of
 * your domain objects.
 * </p>
 *
 * <h2>async and reactive idioms</h2>
 *
 * <p>
 * Rxjava:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(Requery.reactive(Models.DEFAULT));
 *
 *   get("/", () -> {
 *     ReactiveEntityStore store = require(ReactiveEntityStore.class);
 *     // work with reactive store
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Reactor:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(Requery.reactor(Models.DEFAULT));
 *
 *   get("/", () -> {
 *     ReactorEntityStore store = require(ReactorEntityStore.class);
 *     // work with reactor store
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Java 8:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(Requery.completionStage(Models.DEFAULT));
 *
 *   get("/", () -> {
 *     CompletionStageEntityStore store = require(CompletionStageEntityStore.class);
 *     // work with reactor store
 *   });
 * }
 * }</pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * Advanced configuration is available via callback function:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Requery(Models.DEFAULT)
 *     .doWith(builder -> {
 *       builder.useDefaultLogging();
 *       ....
 *     })
 *   );
 * }
 * }</pre>
 *
 * @author edgar
 *
 */
@SuppressWarnings({"rawtypes", "unchecked" })
public class Requery implements Module {

  static {
    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  private final EntityModel model;

  private TableCreationMode schema;

  private final Function<Configuration, EntityStore> store;

  private final Class<? extends EntityStore> storeType;

  private List<Class> states = new LinkedList<>();

  private List<Class> statements = new LinkedList<>();

  private List<Class> transactions = new LinkedList<>();

  private BiConsumer<Config, ConfigurationBuilder> callback;

  private Provider<DataSource> dataSource;

  private Requery(final Class<? extends EntityStore> storeType, final EntityModel model,
      final Function<Configuration, EntityStore> store) {
    this.storeType = storeType;
    this.model = model;
    this.store = store;
  }

  /**
   * Creates a new {@link Requery} module.
   *
   * @param model Entity model.
   */
  public Requery(final EntityModel model) {
    this(EntityStore.class, model, c -> new EntityDataStore<>(c));
  }

  /**
   * Advanced configuration callback:
   *
   * <pre>{@code
   *
   *   use(new Requery(Models.DEFAULT)
   *     .doWith((conf, builder) -> {
   *       builder.useDefaultLogging();
   *     });
   *
   * }</pre>
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Requery doWith(final BiConsumer<Config, ConfigurationBuilder> configurer) {
    this.callback = requireNonNull(configurer, "Configurer callback required.");
    return this;
  }

  /**
   * Advanced configuration callback:
   *
   * <pre>{@code
   *
   *   use(new Requery(Models.DEFAULT)
   *     .doWith(builder -> {
   *       builder.useDefaultLogging();
   *     })
   *   );
   *
   * }</pre>
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Requery doWith(final Consumer<ConfigurationBuilder> configurer) {
    requireNonNull(configurer, "Configurer callback required.");
    return doWith((c, b) -> configurer.accept(b));
  }

  /**
   * Set a custom {@link DataSource}.
   *
   * <pre>{@code
   *
   *   use(new Requery(Models.DEFAULT)
   *     .dataSource(() -> new MySuperDataSource())
   *   );
   *
   * }</pre>
   *
   * @param dataSource DataSource to use.
   * @return This module.
   */
  public Requery dataSource(final Provider<DataSource> dataSource) {
    this.dataSource = requireNonNull(dataSource, "DataSource required.");
    return this;
  }

  /**
   * Run the give schema command at application startup time.
   *
   * @param schema Command to run.
   * @return This module.
   */
  public Requery schema(final TableCreationMode schema) {
    this.schema = schema;
    return this;
  }

  /**
   * Add an {@link EntityStateListener}. The listener will be created by Guice.
   *
   * @param listener Guice entity listener.
   * @return This module.
   */
  public Requery entityStateListener(final Class<? extends EntityStateListener<?>> listener) {
    this.states.add(listener);
    return this;
  }

  /**
   * Add an {@link StatementListener}. The listener will be created by Guice.
   *
   * @param listener Guice statement listener.
   * @return This module.
   */
  public Requery statementListener(final Class<? extends StatementListener> listener) {
    this.statements.add(listener);
    return this;
  }

  /**
   * Add an {@link TransactionListener}. The listener will be created by Guice.
   *
   * @param listener Guice transaction listener.
   * @return This module.
   */
  public Requery transactionListener(final Class<? extends TransactionListener> listener) {
    this.transactions.add(listener);
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    AtomicReference<Object> holder = new AtomicReference<>();
    Provider provider = holder::get;
    Consumer bind = k -> binder.bind((Key) k).toProvider(provider);
    ServiceKey keys = env.serviceKey();
    /**
     * For each model class we publish a type-safe EntityStore, so users can easily inject stores
     * for specific types.
     */
    model.getTypes().forEach(it -> {
      // Person(result) extends AbstractPerson implements Persistable (base)
      Class target = it.getClassType();
      Class base = target.getInterfaces()[0];
      bind.accept(Key.get(Types.newParameterizedType(storeType, base, target)));
    });
    keys.generate(storeType, model.getName(), bind);

    env.onStart(registry -> {
      DataSource ds = Optional.ofNullable(this.dataSource)
          .map(Provider::get)
          .orElseGet(() -> registry.require(DataSource.class));
      schema(conf, schema, schema -> new SchemaModifier(ds, model).createTables(schema));

      ConfigurationBuilder builder = new ConfigurationBuilder(ds, model);
      if (callback != null) {
        callback.accept(conf, builder);
      }
      states
          .forEach(t -> builder.addEntityStateListener((EntityStateListener) registry.require(t)));
      statements
          .forEach(t -> builder.addStatementListener((StatementListener) registry.require(t)));
      transactions.forEach(t -> builder
          .addTransactionListenerFactory(() -> (TransactionListener) registry.require(t)));
      Configuration configuration = builder.build();
      holder.set(store.apply(configuration));
    });
  }

  /**
   * Creates a Requery module with RxJava data store.
   *
   * <pre>{@code
   * {
   *   use(Requery.reactive(Models.DEFAULT));
   *
   *   get("/", () -> {
   *     ReactiveEntityStore store = require(ReactiveEntityStore.class);
   *     // work with reactive store
   *   });
   * }
   * }</pre>
   *
   * @param model Entity model.
   * @return A new {@link Requery} module.
   */
  public static Requery reactive(final EntityModel model) {
    return new Requery(ReactiveEntityStore.class, model,
        conf -> ReactiveSupport.toReactiveStore(new EntityDataStore<>(conf)));
  }

  /**
   * Creates a Requery module with Reactor data store.
   *
   * <pre>{@code
   * {
   *   use(Requery.reactive(Models.DEFAULT));
   *
   *   get("/", () -> {
   *     ReactorEntityStore store = require(ReactorEntityStore.class);
   *     // work with reactive store
   *   });
   * }
   * }</pre>
   *
   * @param model Entity model.
   * @return A new {@link Requery} module.
   */
  public static Requery reactor(final EntityModel model) {
    return new Requery(ReactorEntityStore.class, model,
        conf -> new ReactorEntityStore<>(new EntityDataStore<>(conf)));
  }

  /**
   * Creates a Requery module with Java 8 data store.
   *
   * <pre>{@code
   * {
   *   use(Requery.reactive(Models.DEFAULT));
   *
   *   get("/", () -> {
   *     CompletionStageEntityStore store = require(CompletionStageEntityStore.class);
   *     // work with reactive store
   *   });
   * }
   * }</pre>
   *
   * @param model Entity model.
   * @return A new {@link Requery} module.
   */
  public static Requery completionStage(final EntityModel model) {
    return new Requery(CompletionStageEntityStore.class, model,
        conf -> new CompletableEntityStore(new EntityDataStore<>(conf)));
  }

  private void schema(final Config conf, final TableCreationMode schema,
      final Consumer<TableCreationMode> callback) {
    if (schema != null) {
      callback.accept(schema);
    }
    if (conf.hasPath("requery.schema")) {
      callback.accept(TableCreationMode.valueOf(conf.getString("requery.schema").toUpperCase()));
    }
  }

}
