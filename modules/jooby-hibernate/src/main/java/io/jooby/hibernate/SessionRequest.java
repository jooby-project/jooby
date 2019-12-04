/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.Route;
import io.jooby.ServiceKey;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Attach {@link Session} and {@link javax.persistence.EntityManager} to the current request.
 *
 * The active {@link Session} is accessible via {@link SessionFactory#getCurrentSession()} for the
 * duration of the route pipeline.
 *
 * Once route pipeline is executed the session/entityManager is detached from current request and
 * closed.
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
 *   decorator(new SessionRequest());
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
 * @author Benjamin Quinn
 */
public class SessionRequest implements Route.Decorator  {

  private static final Logger log = LoggerFactory.getLogger(SessionRequest.class);

  private final ServiceKey<SessionFactory> key;

  /**
   * Creates a new session request and attach the to a named session factory.
   *
   * @param name Name of the session factory.
   */
  public SessionRequest(@Nonnull String name) {
    key = ServiceKey.key(SessionFactory.class, name);
  }

  /**
   * Creates a new session request and attach to the default/first session factory registered.
   */
  public SessionRequest() {
    key = ServiceKey.key(SessionFactory.class);
  }

  @Nonnull
  @Override
  public Route.Handler apply(@Nonnull Route.Handler next) {
    return (ctx) -> {
      SessionFactory sessionFactory = ctx.require(key);
      try {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        Object result = next.apply(ctx);

        Transaction transaction = session.getTransaction();
        if (transaction.getStatus() == TransactionStatus.ACTIVE) {
          log.error("Transaction state is still active (expected to be committed, or rolled "
              + "back) after route pipeline completed, rolling back.");

          transaction.rollback();
        }

        return result;
      } finally {
        Session session = ManagedSessionContext.unbind(sessionFactory);
        if (session != null) {
          sessionFactory.close();
        }
      }
    };
  }
}

