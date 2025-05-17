/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.internal.hibernate.RequestSessionFactory;

/**
 * Attach {@link Session} and {@link jakarta.persistence.EntityManager} to the current request.
 *
 * <p>The active {@link Session} is accessible via {@link SessionFactory#getCurrentSession()} for
 * the duration of the route pipeline.
 *
 * <p>Once route pipeline is executed the session/entityManager is detached from current request and
 * closed.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 *   install(new HibernateModule());
 *
 *   use(new SessionRequest());
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
public class SessionRequest implements Route.Filter {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ServiceKey<SessionFactory> sessionFactoryKey;

  private final RequestSessionFactory sessionProvider;

  /**
   * Creates a new session request and attach the to a named session factory.
   *
   * @param name Name of the session factory.
   */
  public SessionRequest(@NonNull String name) {
    this(ServiceKey.key(SessionFactory.class, name));
  }

  /** Creates a new session request and attach to the default/first session factory registered. */
  public SessionRequest() {
    this(ServiceKey.key(SessionFactory.class));
  }

  private SessionRequest(ServiceKey<SessionFactory> sessionFactoryKey) {
    this.sessionFactoryKey = sessionFactoryKey;
    this.sessionProvider =
        RequestSessionFactory.stateful(
            ServiceKey.key(SessionProvider.class, sessionFactoryKey.getName()));
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      var sessionFactory = ctx.require(sessionFactoryKey);
      try (var session = sessionProvider.create(ctx, sessionFactory)) {

        Object result = next.apply(ctx);

        Transaction transaction = session.getTransaction();
        if (transaction.getStatus() == TransactionStatus.ACTIVE) {
          log.error(
              "Transaction state is still active (expected to be committed, or rolled "
                  + "back) after route pipeline completed, rolling back.");

          transaction.rollback();
        }

        return result;
      } finally {
        sessionProvider.release(sessionFactory);
      }
    };
  }

  /**
   * Get the service key for accessing to the configured {@link SessionFactory} service.
   *
   * @return The service key for accessing to the configured {@link SessionFactory} service.
   */
  public @NonNull ServiceKey<SessionFactory> getSessionFactoryKey() {
    return sessionFactoryKey;
  }
}
