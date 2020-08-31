/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.SneakyThrows;
import io.jooby.annotations.Transactional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.annotation.Nonnull;

/**
 * Attaches a {@link Session} and {@link javax.persistence.EntityManager} to the current request
 * via {@link SessionRequest}.
 *
 * The route pipeline runs inside a transaction which is commit on success or rollback in case of
 * exception.
 *
 * Applies the {@link SessionRequest} decorator, so there is no need to use session request in
 * addition to transactional request.
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
 * @author edgar
 * @since 2.0.0
 */
public class TransactionalRequest implements Route.Decorator {

  private SessionRequest sessionRequest;

  private ServiceKey<SessionFactory> key;

  private boolean enabledByDefault = true;

  /**
   * Creates a new transactional request and attach the to a named session factory.
   *
   * @param name Name of the session factory.
   */
  public TransactionalRequest(@Nonnull String name) {
    this(new SessionRequest(name));
  }

  /**
   * Creates a new transactional request and attach to the default/first session factory registered.
   */
  public TransactionalRequest() {
    this(new SessionRequest());
  }

  /**
   * Creates a new transactional request.
   *
   * @param sessionRequest Session request instance.
   */
  public TransactionalRequest(@Nonnull SessionRequest sessionRequest) {
    this.sessionRequest = sessionRequest;
  }

  /**
   * Sets whether all routes in the scope of this decorator instance
   * should be transactional or not ({@code true} by default).
   * <p>
   * You can use the {@link Transactional} annotation to override this
   * option on a single route.
   *
   * @param enabledByDefault whether routes should be transactional by default
   * @return this instance
   * @see Transactional
   */
  public TransactionalRequest enabledByDefault(boolean enabledByDefault) {
    this.enabledByDefault = enabledByDefault;
    return this;
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return sessionRequest.apply(ctx -> {
      if (ctx.getRoute().isTransactional(enabledByDefault)) {
        SessionFactory sessionFactory = ctx.require(sessionRequest.getSessionFactoryKey());
        Transaction trx = null;
        try {
          Session session = sessionFactory.getCurrentSession();
          trx = session.getTransaction();
          trx.begin();

          Object result = next.apply(ctx);

          if (trx.isActive()) {
            trx.commit();
          }

          return result;
        } catch (Throwable ex) {
          if (trx != null && trx.isActive()) {
            trx.rollback();
          }
          throw SneakyThrows.propagate(ex);
        }
      } else {
        return next.apply(ctx);
      }
    });
  }
}
