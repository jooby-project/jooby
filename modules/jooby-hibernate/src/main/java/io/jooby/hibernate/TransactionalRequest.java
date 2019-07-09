/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.annotation.Nonnull;

/**
 * Attach {@link Session} and {@link javax.persistence.EntityManager} to the current request.
 * The route pipeline runs inside a transaction which is commit on success or rollback in case of
 * exception.
 *
 * Once route pipeline is executed the session/entityManager is detached from current request and
 * closed it.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 *   install(new HibernateModule());
 *
 *   decorator(new TransactionalRequest());
 *
 *   get("/handle", ctx -> {
 *     EntityManager handle = require(EntityManager.class);
 *     // work with handle.
 *   });
 * }
 * }</pre>
 *
 * NOTE: This is NOT the open session in view pattern. Persistent objects must be fully initialized
 * to be encoded/rendered to the client. Otherwise, Hibernate results in
 * LazyInitializationException.
 *
 * @author edgar
 * @since 2.0.0
 */
public class TransactionalRequest implements Route.Decorator {

  private ServiceKey<SessionFactory> key;

  /**
   * Creates a new transactional request and attach the to a named session factory.
   *
   * @param name Name of the session factory.
   */
  public TransactionalRequest(@Nonnull String name) {
    key = ServiceKey.key(SessionFactory.class, name);
  }

  /**
   * Creates a new transactional request and attach to the default/first session factory registered.
   */
  public TransactionalRequest() {
    key = ServiceKey.key(SessionFactory.class);
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      SessionFactory sessionFactory = ctx.require(key);
      Transaction trx = null;
      try {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        trx = session.getTransaction();
        trx.begin();

        Object result = next.apply(ctx);

        if (trx.isActive()) {
          trx.commit();
        }
        return result;
      } catch (Throwable x) {
        if (trx != null && trx.isActive()) {
          trx.rollback();
        }
        throw SneakyThrows.propagate(x);
      } finally {
        Session session = ManagedSessionContext.unbind(sessionFactory);
        if (session != null) {
          session.close();
        }
      }
    };
  }
}
