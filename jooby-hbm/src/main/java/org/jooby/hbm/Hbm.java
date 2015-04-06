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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.jpa.AvailableSettings;
import org.jooby.Env;
import org.jooby.Route;
import org.jooby.internal.hbm.HbmProvider;
import org.jooby.internal.hbm.HbmUnitDescriptor;
import org.jooby.jdbc.Jdbc;
import org.jooby.scope.RequestScoped;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <p>
 * Object-Relational-Mapping via Hibernate. Exposes an {@link EntityManagerFactory} and
 * {@link EntityManager}.
 * </p>
 * <p>
 * This module extends {@link Jdbc} module, before going forward, make sure you read the doc of the
 * {@link Jdbc} module first.
 * </p>
 *
 * <p>
 * This module provides an advanced and recommended <a href=
 * "https://developer.jboss.org/wiki/OpenSessionInView#jive_content_id_Can_I_use_two_transactions_in_one_Session"
 * >Open Session in View</a> pattern, which basically keep the {@link Session} opened until the view
 * is rendered, but it uses two database transactions: 1) first transaction is committed before
 * rendering the view and then 2) a read only transaction is opened for rendering the view.
 * </p>
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Hbm(EntityA.class, EntityB.class));
 *
 *   get("/", req {@literal ->} {
 *    EntityManager em = req.require(EntityManager.class);
 *    // work with em...
 *   });
 * }
 * </pre>
 *
 * At bootstrap time you will see something similar to this:
 *
 * <pre>
 * {@literal *} /{@literal *}{@literal *} [{@literal *}/{@literal *}] [{@literal *}/{@literal *}] (hbm)
 * </pre>
 *
 * That is the filter with the <strong>Open Session in View</strong> pattern.
 *
 * <h1>life-cycle</h1>
 *
 * <p>
 * You are free to inject an {@link EntityManagerFactory} create a new
 * {@link EntityManagerFactory#createEntityManager()}, start transactions and do everything you
 * need.
 * </p>
 *
 * <p>
 * For the time being, this doesn't work for an {@link EntityManager}. An {@link EntityManager} is
 * bound to the current request, which means you can't freely access from every single thread (like
 * manually started thread, started by an executor service, quartz, etc...).
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
 * This is because the {@link EntityManager} is bound as {@link RequestScoped}.
 *
 * <p>
 * Still, we strongly recommend to leave your services in the default scope and avoid to use
 * {@link Singleton} objects, except of course for really expensive resources. This is also
 * recommend it by Guice.
 * </p>
 *
 * <p>
 * Services in the default scope won't have this problem and are free to inject the
 * {@link EntityManager} directly.
 * </p>
 *
 * <h1>persistent classes</h1>
 * <p>
 * Classpath scanning is OFF by default, so you need to explicitly tell Hibernate which classes are
 * persistent. This intentional and helps to reduce bootstrap time and have explicit control over
 * persistent classes.
 * </p>
 * <p>
 * If you don't care about bootstrap time and/or just like the auto-discover feature, just do:
 * </p>
 *
 * <pre>
 * {
 *   use(new Hbm().scan());
 * }
 * </pre>
 *
 * After calling {@link #scan()}, Hibernate will auto-discover all the entities application's
 * namespace. The namespace is defined by the package of your application. Given:
 * <code>org.myproject.App</code> it will scan everthing under <code>org.myproject</code>.
 *
 * <h1>options</h1>
 *
 * <p>
 * Hibernate options can be set from your <code>application.conf</code> file, just make sure to
 * prefix them with <code>hibernate.*</code>
 * </p>
 *
 * <pre>
 * hibernate.hbm2ddl.auto = update
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 */
public class Hbm extends Jdbc {

  private final List<Class<?>> classes = new LinkedList<>();

  private boolean scan = false;

  private HbmProvider emf;

  public Hbm(final String name, final Class<?>... classes) {
    super(name);
    this.classes.addAll(Arrays.asList(classes));
  }

  public Hbm(final Class<?>... classes) {
    this.classes.addAll(Arrays.asList(classes));
  }

  public Hbm scan() {
    this.scan = true;
    return this;
  }

  @Override
  public Config config() {
    Config jdbc = super.config();
    return ConfigFactory.parseResources(getClass(), "hbm.conf").withFallback(jdbc);
  }

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {
    super.configure(mode, config, binder);

    HbmUnitDescriptor descriptor = new HbmUnitDescriptor(getClass().getClassLoader(), dataSource(),
        config, scan);

    emf = new HbmProvider(descriptor, config(config, classes));
    keys(EntityManagerFactory.class, key -> binder.bind(key).toProvider(emf).asEagerSingleton());

    List<Key<EntityManager>> emkeys = new ArrayList<>();

    keys(EntityManager.class, key -> {
      Provider<EntityManager> em = () -> {
        throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block");
      };
      binder.bind(key).toProvider(em).in(RequestScoped.class);
      emkeys.add(key);
    });

    Multibinder<Route.Definition> routes = Multibinder.newSetBinder(binder, Route.Definition.class);
    routes.addBinding().toInstance(
        new Route.Definition("*", "*", new OpenSessionInView(emf, emkeys)).name("hbm")
        );
  }

  private static Map<Object, Object> config(final Config config, final List<Class<?>> classes) {
    Map<Object, Object> $ = new HashMap<>();
    config.getConfig("hibernate")
        .entrySet()
        .forEach(e -> $.put("hibernate." + e.getKey(), e.getValue().unwrapped()));

    if (classes.size() > 0) {
      $.put(AvailableSettings.LOADED_CLASSES, classes);
    }

    return $;
  }

}
