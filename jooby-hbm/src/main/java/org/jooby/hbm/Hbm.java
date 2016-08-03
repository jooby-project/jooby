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
package org.jooby.hbm;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Registry;
import org.jooby.Route;
import org.jooby.internal.hbm.GuiceBeanManager;
import org.jooby.internal.hbm.OpenSessionInView;
import org.jooby.internal.hbm.ScanEnvImpl;
import org.jooby.internal.hbm.SessionProvider;
import org.jooby.internal.hbm.UnitOfWorkProvider;
import org.jooby.jdbc.Jdbc;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.concurrent.Promise;

/**
 * <h1>hibernate</h1>
 * <p>
 * <a href="http://hibernate.org/orm">Hibernate ORM</a> enables developers to more easily write
 * applications whose data outlives the application process. As an Object/Relational Mapping (ORM)
 * framework, Hibernate is concerned with data persistence as it applies to relational databases.
 * </p>
 * <p>
 * This module setup and configure <a href="http://hibernate.org/orm">Hibernate ORM</a> and
 * <code>JPA Provider</code>.
 * </p>
 *
 * <p>
 * This module depends on {@link Jdbc} module, make sure you read the doc of the {@link Jdbc}
 * module.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>SessionFactory / EntityManagerFactory</li>
 * <li>Session / EntityManager</li>
 * <li>UnitOfWork</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Hbm("jdbc:mysql://localhost/mydb")
 *       .classes(Beer.class)
 *   );
 *
 *   get("/api/beer/", req -> {
 *     return require(UnitOfWork.class).apply(em -> {
 *       return em.createQuery("from Beer").getResultList();
 *     });
 *   });
 * }
 * }</pre>
 *
 * <h2>unit of work</h2>
 * <p>
 * We provide an {@link UnitOfWork} to simplify the amount of code required to interact within the
 * database.
 * </p>
 * <p>
 * For example the next line:
 * </p>
 *
 * <pre>{@code
 * {
 *   require(UnitOfWork.class).apply(em -> {
 *     return em.createQuery("from Beer").getResultList();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Is the same as:
 * </p>
 *
 * <pre>{@code
 * {
 *    Session session = require(SessionFactory.class).openSession();
 *    Transaction trx = session.getTransaction();
 *    try {
 *      trx.begin();
 *      List<Beer> beers = em.createQuery("from Beer").getResultList();
 *      trx.commit();
 *    } catch (Exception ex) {
 *      trx.rollback();
 *    } finally {
 *      session.close();
 *    }
 * }
 * }</pre>
 *
 * <p>
 * An {@link UnitOfWork} takes care of transactions and session life-cycle. It's worth to mention
 * too that a first requested {@link UnitOfWork} bind the Session to the current request. If later
 * in the execution flow an {@link UnitOfWork}, {@link Session} and/or {@link EntityManager} is
 * required then the one that belong to the current request (first requested) will be provided it.
 * </p>
 *
 * <h2>open session in view</h2>
 * <p>
 * We provide an advanced and recommended <a href=
 * "https://developer.jboss.org/wiki/OpenSessionInView#jive_content_id_Can_I_use_two_transactions_in_one_Session"
 * >Open Session in View</a> pattern, which basically keep the {@link Session} opened until the view
 * is rendered, but it uses two database transactions:
 * </p>
 *
 * <ol>
 * <li>first transaction is committed before rendering the view and then</li>
 * <li>a read only transaction is opened for rendering the view</li>
 * </ol>
 *
 * <p>
 * Here is an example on how to setup the open session in view filter:
 * </p>
 *
 * <pre>{@code
 * {
 *    use(new Hbm());
 *
 *    use("*", Hbm.openSessionInView());
 * }
 * }</pre>
 *
 * <h2>event listeners</h2>
 * <p>
 * JPA event listeners are provided by Guice, which means you can inject dependencies into your
 * event
 * listeners:
 * </p>
 *
 * <pre>{@code
 *
 * &#64;Entity
 * &#64;EntityListeners({BeerListener.class})
 * public class Beer {
 *
 * }
 *
 * public class BeerListener {
 *   &#64;Inject
 *   public BeerListener(DependencyA depA) {
 *     this.depA = depA;
 *   }
 *
 *   &#64;PostLoad
 *   public void postLoad(Beer beer) {
 *     this.depA.complementBeer(beer);
 *   }
 * }
 *
 * }</pre>
 *
 * <p>
 * Hibernate event listeners are supported too via {@link #onEvent(EventType, Class)}:
 * </p>
 *
 * <pre>{@code
 * {
 *    use(new Hbm()
 *        .onEvent(EventType.POST_LOAD, MyPostLoadListener.class));
 * }
 * }</pre>
 *
 * <p>
 * Again, <code>MyPostLoadListener</code> will be provided by Guice.
 * </p>
 *
 * <h2>persistent classes</h2>
 * <p>
 * Persistent classes must be provided at application startup time via
 * {@link #classes(Class...)}:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Hbm()
 *       .classes(Entity1.class, Entity2.class, ..., )
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Or via {@link #scan()}:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Hbm()
 *       .scan()
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Which <code>scan</code> the application package, or you can provide where to look:
 * <p>
 *
 * <pre>{@code
 * {
 *   use(new Hbm()
 *       .scan("foo.bar", "x.y.z")
 *   );
 * }
 * }</pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * Advanced configuration is provided via {@link #doWith(Consumer)} callbacks:
 * </p>
 * <pre>{@code
 * {
 *   use(new Hbm()
 *       .doWith((BootstrapServiceRegistryBuilder bsrb) -> {
 *         // do with bsrb
 *       })
 *       .doWith((StandardServiceRegistryBuilder ssrb) -> {
 *         // do with ssrb
 *       })
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Or via <code>hibernate.*</code> property from your <code>.conf</code> file:
 * </p>
 *
 * <pre>{@code
 *   hibernate.hbm2ddl.auto = update
 * }</pre>
 *
 *
 * <h2>life-cycle</h2>
 * <p>
 * You are free to inject a {@link SessionFactory} or {@link EntityManagerFactory} create a new
 * {@link EntityManagerFactory#createEntityManager()}, start transactions and do everything you
 * need.
 * </p>
 *
 * <p>
 * For the time being, this doesn't work for a {@link Session} or {@link EntityManager}. A
 * {@link Session} {@link EntityManager} is bound to the current request, which means you can't
 * freely access from every single thread (like manually started thread, started by an executor
 * service, quartz, etc...).
 * </p>
 *
 * <p>
 * Another restriction, is the access from {@link Singleton} services. If you need access from a
 * singleton services, you need to inject a {@link Provider}.
 * </p>
 *
 * <pre>
 *
 * &#64;Singleton
 * public class MySingleton {
 *
 *   &#64;Inject
 *   public MySingleton(Provider&lt;EntityManager&gt; em) {
 *     this.em = em;
 *   }
 * }
 * </pre>
 *
 * <p>
 * Still, we strongly recommend to leave your services in the default scope and avoid to use
 * {@link Singleton} objects, except of course for really expensive resources. This is also
 * recommend it by Guice.
 * </p>
 *
 * <p>
 * Services in the default scope won't have this problem and are free to inject the
 * {@link Session} or {@link EntityManager} directly.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public class Hbm extends Jdbc {

  private List<BiConsumer<SessionFactoryImplementor, Registry>> listeners = new ArrayList<>();

  private List<Consumer<Binder>> bindings = new ArrayList<>();

  private List<BiConsumer<MetadataSources, Config>> sources = new ArrayList<>();

  /**
   * Creates a new {@link Hbm} module.
   *
   * @param db A jdbc connection string or a property with a jdbc connection string.
   */
  public Hbm(final String db) {
    super(db);
  }

  /**
   * Creates a new {@link Hbm} module. A <code>db</code> property must be present in your
   * <code>.conf</code> file.
   */
  public Hbm() {
  }

  /**
   * Append persistent classes (classess annotated with Entity).
   *
   * @param classes Persistent classes.
   * @return This module.
   */
  @SuppressWarnings("rawtypes")
  public Hbm classes(final Class... classes) {
    sources.add((m, c) -> Arrays.asList(classes).stream().forEach(m::addAnnotatedClass));
    return this;
  }

  /**
   * Scan the provided packages and discover persistent classes (annotated with Entity).
   *
   * @param packages Package to scan.
   * @return This module.
   */
  public Hbm scan(final String... packages) {
    sources.add((m, c) -> Arrays.asList(packages).stream().forEach(m::addPackage));
    return this;
  }

  /**
   * Scan the application package (that's the package where you application was defined) and
   * discover persistent classes (annotated with Entity).
   *
   * @return This module.
   */
  public Hbm scan() {
    sources.add((m, c) -> m.addPackage(c.getString("application.ns")));
    return this;
  }

  /**
   * Creates an open session in view filter as described <a href=
   * "https://developer.jboss.org/wiki/OpenSessionInView#jive_content_id_Can_I_use_two_transactions_in_one_Session">here</a>.
   *
   * Please note a call to this method give you only the filter, you must add it to your application
   * like:
   *
   * <pre>{@code
   *  {
   *     use(new Hbm());
   *
   *     use("*", Hbm.openSessionInView());
   *  }
   * }</pre>
   *
   * @return This module.
   */
  public static Route.Filter openSessionInView() {
    return new OpenSessionInView();
  }

  /**
   * Register an hibernate event listener. Listener will be created and injected by Guice.
   *
   * @param type Event type.
   * @param listenerType Listener type.
   * @return This module.
   */
  @SuppressWarnings("unchecked")
  public <T> Hbm onEvent(final EventType<T> type, final Class<? extends T> listenerType) {
    bindings.add(b -> {
      b.bind(listenerType).asEagerSingleton();
    });

    listeners.add((s, r) -> {
      ServiceRegistryImplementor serviceRegistry = s.getServiceRegistry();
      EventListenerRegistry service = serviceRegistry.getService(EventListenerRegistry.class);
      T listener = r.require(listenerType);
      service.appendListeners(type, listener);
    });
    return this;
  }

  /**
   * Configurer callback to apply advanced configuration while bootstrapping hibernate:
   *
   * <pre>{@code
   * {
   *   use(new Hbm()
   *       .doWith((BootstrapServiceRegistryBuilder bsrb, Config conf) -> {
   *         // do with bsrb
   *       })
   *       .doWith((StandardServiceRegistryBuilder ssrb, Config conf) -> {
   *         // do with ssrb
   *       })
   *   );
   * }
   * }</pre>
   *
   * @param configurer Configurer callback.
   * @return This module
   */
  @Override
  public <T> Hbm doWith(final BiConsumer<T, Config> configurer) {
    super.doWith(configurer);
    return this;
  }

  /**
   * Configurer callback to apply advanced configuration while bootstrapping hibernate:
   *
   * <pre>{@code
   * {
   *   use(new Hbm()
   *       .doWith((BootstrapServiceRegistryBuilder bsrb) -> {
   *         // do with bsrb
   *       })
   *       .doWith((StandardServiceRegistryBuilder ssrb) -> {
   *         // do with ssrb
   *       })
   *   );
   * }
   * }</pre>
   *
   * @param configurer Configurer callback.
   * @return This module
   */
  @Override
  public <T> Hbm doWith(final Consumer<T> configurer) {
    super.doWith(configurer);
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    super.configure(env, conf, binder, (name, ds) -> {
      BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
      bsrb.applyIntegrator(new JpaIntegrator());

      callback(bsrb, conf);

      String ddl_auto = env.name().equals("dev") ? "update" : "none";

      BootstrapServiceRegistry bsr = bsrb.build();
      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder(bsr);
      ssrb.applySetting(AvailableSettings.HBM2DDL_AUTO, ddl_auto);

      ssrb.applySettings(settings(env, conf));

      callback(ssrb, conf);

      ssrb.applySetting(AvailableSettings.DATASOURCE, ds);
      ssrb.applySetting(org.hibernate.jpa.AvailableSettings.DELAY_CDI_ACCESS, true);

      StandardServiceRegistry serviceRegistry = ssrb.build();

      MetadataSources sources = new MetadataSources(serviceRegistry);
      this.sources.forEach(src -> src.accept(sources, conf));
      callback(sources, conf);

      /** scan package? */
      List<URL> packages = sources.getAnnotatedPackages()
          .stream()
          .map(pkg -> getClass().getResource("/" + pkg.replace('.', '/')))
          .collect(Collectors.toList());

      Metadata metadata = sources.getMetadataBuilder()
          .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
          .applyScanEnvironment(new ScanEnvImpl(packages))
          .build();

      SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
      callback(sfb, conf);
      sfb.applyName(name);

      Promise<Registry> registry = Promise.make();
      sfb.applyBeanManager(GuiceBeanManager.beanManager(registry));

      SessionFactory sessionFactory = sfb.build();
      callback(sessionFactory, conf);

      Provider<Session> session = new SessionProvider(sessionFactory);

      ServiceKey serviceKey = env.serviceKey();
      serviceKey.generate(SessionFactory.class, name,
          k -> binder.bind(k).toInstance(sessionFactory));
      serviceKey.generate(EntityManagerFactory.class, name,
          k -> binder.bind(k).toInstance(sessionFactory));

      /** Session/Entity Manager . */
      serviceKey.generate(Session.class, name,
          k -> binder.bind(k).toProvider(session));
      serviceKey.generate(EntityManager.class, name,
          k -> binder.bind(k).toProvider(session));

      /** Unit of work . */
      Provider<UnitOfWork> uow = new UnitOfWorkProvider(sessionFactory);
      serviceKey.generate(UnitOfWork.class, name,
          k -> binder.bind(k).toProvider(uow));

      bindings.forEach(it -> it.accept(binder));

      env.onStart(r -> {
        registry.success(r);
        listeners.forEach(it -> it.accept((SessionFactoryImplementor) sessionFactory, r));
      });

      env.onStop(sessionFactory::close);
    });
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "hbm.conf").withFallback(super.config());
  }

  private static Map<Object, Object> settings(final Env env, final Config config) {
    Map<Object, Object> $ = new HashMap<>();
    config.getConfig("hibernate")
        .entrySet()
        .forEach(e -> $.put("hibernate." + e.getKey(), e.getValue().unwrapped()));

    return $;
  }
}
