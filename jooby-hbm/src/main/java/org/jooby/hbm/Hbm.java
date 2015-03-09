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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.jooby.Body;
import org.jooby.Env;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.internal.hbm.HbmUnitDescriptor;
import org.jooby.jdbc.Jdbc;
import org.jooby.scope.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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
    Logger log = LoggerFactory.getLogger(getClass());

    super.configure(mode, config, binder);

    HbmUnitDescriptor descriptor = new HbmUnitDescriptor(getClass().getClassLoader(), dataSource(),
        config, scan);

    EntityManagerFactoryBuilder builder = Bootstrap
        .getEntityManagerFactoryBuilder(descriptor, config(config, classes));

    emf = new HbmProvider(builder);
    Key<EntityManagerFactory> emfKey = dataSourceKey(EntityManagerFactory.class);

    binder.bind(emfKey).toProvider(emf);

    Key<EntityManager> emKey = dataSourceKey(EntityManager.class);

    Multibinder<Route.Definition> routes = Multibinder.newSetBinder(binder, Route.Definition.class);

    routes.addBinding()
        .toInstance(new Route.Definition("*", "*", readWriteTrx(emf, emKey, log)).name("hbm"));

    binder.bind(emKey).toProvider(() -> {
      throw new OutOfScopeException("Cannot access " + emKey + " outside of a scoping block");
    }).in(RequestScoped.class);
  }

  private static Route.Filter readWriteTrx(final HbmProvider emf, final Key<EntityManager> key,
      final Logger log) {
    return (req, resp, chain) -> {
      log.debug("creating entity manager: {}", key);
      EntityManager em = emf.get().createEntityManager();
      req.set(key, em);
      Session session = (Session) em.getDelegate();
      FlushMode flushMode = FlushMode.AUTO;
      log.debug("setting flush mode to: {}", flushMode);
      session.setFlushMode(flushMode);
      log.debug("  creating new transaction");
      EntityTransaction trx = em.getTransaction();
      try {
        log.debug("  starting transaction: {}", trx);
        trx.begin();

        // invoke next handler
        chain.next(req, new Response.Forwarding(resp) {

          void setReadOnly(final Session session, final boolean readOnly) {
            try {
              Connection connection = ((SessionImplementor) session).connection();
              connection.setReadOnly(readOnly);
            } catch (SQLException ignored) {
              log.trace("  isn't possible to mark current connection as read-only", ignored);
            }
          }

          void transactionalSend(final Callable<Void> send) throws Exception {
            try {
              Session session = (Session) em.getDelegate();
              log.debug("  flushing session: {}", session);
              session.flush();

              if (trx.isActive()) {
                log.debug("  commiting trx: {}", trx);
                trx.commit();
              }

              // start new transaction
              log.debug("  setting connection to read only");
              setReadOnly(session, true);
              session.setFlushMode(FlushMode.MANUAL);
              EntityTransaction readOnlyTrx = em.getTransaction();
              log.debug("  starting readonly trx: {}", readOnlyTrx);
              readOnlyTrx.begin();

              try {
                // send it!
                send.call();

                log.debug("  commiting readonly trx: {}", readOnlyTrx);
                readOnlyTrx.commit();
              } catch (Exception ex) {
                if (readOnlyTrx.isActive()) {
                  log.debug("  rolling back readonly trx: {}", readOnlyTrx);
                  readOnlyTrx.rollback();
                }
                throw ex;
              }
            } finally {
              log.debug("  removing readonly mode from connection");
              setReadOnly(session, false);
            }
          }

          @Override
          public void send(final Object body) throws Exception {
            transactionalSend(() -> {
              super.send(body);
              return null;
            });

          }

          @Override
          public void send(final Body body) throws Exception {
            transactionalSend(() -> {
              super.send(body);
              return null;
            });
          }

        });
      } finally {
        try {
          if (trx.isActive()) {
            log.debug("  rolling back transation: {}", trx);
            trx.rollback();
          }
        } finally {
          log.debug("closing entity manager: {}", em);
          em.close();
        }
      }
    };
  }

  @Override
  public void start() {
    super.start();
    emf.start();
  }

  @Override
  public void stop() {
    if (emf != null) {
      emf.stop();
    }
    super.stop();
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
