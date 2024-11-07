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
import io.jooby.*;
import io.jooby.annotation.Transactional;
import io.jooby.internal.hibernate.RequestSessionFactory;

/**
 * Attaches a {@link Session} and {@link jakarta.persistence.EntityManager} to the current request
 * via {@link SessionRequest}.
 *
 * <p>The route pipeline runs inside a transaction which is commit on success or rollback in case of
 * exception.
 *
 * <p>Applies the {@link SessionRequest} decorator, so there is no need to use session request in
 * addition to transactional request.
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
 *   use(new TransactionalRequest());
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
public class TransactionalRequest implements Route.Filter {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ServiceKey<SessionFactory> sessionFactoryKey;

  private RequestSessionFactory sessionProvider;

  private boolean enabledByDefault = true;

  /**
   * Creates a new transactional request and attach the to a named session factory.
   *
   * @param name Name of the session factory.
   */
  public TransactionalRequest(@NonNull String name) {
    this(ServiceKey.key(SessionFactory.class, name));
  }

  /**
   * Creates a new transactional request and attach to the default/first session factory registered.
   */
  public TransactionalRequest() {
    this(ServiceKey.key(SessionFactory.class));
  }

  private TransactionalRequest(ServiceKey<SessionFactory> sessionFactoryKey) {
    this.sessionFactoryKey = sessionFactoryKey;
    this.sessionProvider =
        RequestSessionFactory.stateful(
            ServiceKey.key(SessionProvider.class, sessionFactoryKey.getName()));
  }

  /**
   * Sets whether all routes in the scope of this decorator instance should be transactional or not
   * ({@code true} by default).
   *
   * <p>You can use the {@link Transactional} annotation to override this option on a single route.
   *
   * @param enabledByDefault whether routes should be transactional by default
   * @return this instance
   * @see Transactional
   */
  public TransactionalRequest enabledByDefault(boolean enabledByDefault) {
    this.enabledByDefault = enabledByDefault;
    return this;
  }

  /**
   * Creates a {@link org.hibernate.StatelessSession} and attach to current HTTP request.
   *
   * @return This instance.
   */
  public TransactionalRequest useStatelessSession() {
    this.sessionProvider =
        RequestSessionFactory.stateless(
            ServiceKey.key(StatelessSessionProvider.class, sessionFactoryKey.getName()));
    return this;
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      if (ctx.getRoute().isTransactional(enabledByDefault)) {
        var sessionFactory = ctx.require(sessionFactoryKey);
        try (var session = sessionProvider.create(ctx, sessionFactory)) {

          Object result;

          Transaction trx = null;
          try {
            trx = session.getTransaction();
            trx.begin();

            result = next.apply(ctx);

            if (trx.isActive()) {
              trx.commit();
            }
          } catch (Throwable ex) {
            if (trx != null && trx.isActive()) {
              trx.rollback();
            }
            throw SneakyThrows.propagate(ex);
          }

          ensureCompletion(session.getTransaction());

          return result;
        } finally {
          sessionProvider.release(sessionFactory);
        }
      } else {
        return next.apply(ctx);
      }
    };
  }

  private void ensureCompletion(Transaction transaction) {
    if (transaction.getStatus() == TransactionStatus.ACTIVE) {
      log.error(
          "Transaction state is still active (expected to be committed, or rolled "
              + "back) after route pipeline completed, rolling back.");

      transaction.rollback();
    }
  }
}
