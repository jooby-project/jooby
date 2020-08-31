/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.ebean;

import io.ebean.Database;
import io.ebean.Transaction;
import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.annotations.Transactional;

import javax.annotation.Nonnull;

/**
 * Start a new transaction on each incoming request. Its commit the transaction is no exception is
 * thrown or rollback in case of an exception.
 *
 * @author edgar.
 */
public class TransactionalRequest implements Route.Decorator {

  private ServiceKey<Database> key;

  private boolean enabledByDefault = true;

  /**
   * Creates a transactional request.
   *
   * This constructor should be used only if you have multiple Ebean installations.
   *
   * @param name Ebean service name.
   */
  public TransactionalRequest(@Nonnull String name) {
    key = ServiceKey.key(Database.class, name);
  }

  /**
   * Creates a transactional request.
   */
  public TransactionalRequest() {
    key = ServiceKey.key(Database.class);
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
    return ctx -> {
      if (ctx.getRoute().isTransactional(enabledByDefault)) {
        Database db = ctx.require(key);
        try (Transaction transaction = db.beginTransaction()) {
          Object result = next.apply(ctx);
          transaction.commit();
          return result;
        }
      } else {
        return next.apply(ctx);
      }
    };
  }
}
