/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.ebean;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.ebean.Database;
import io.jooby.Route;
import io.jooby.ServiceKey;
import io.jooby.annotation.Transactional;

/**
 * Start a new transaction on each incoming request. Its commit the transaction is no exception is
 * thrown or rollback in case of an exception.
 *
 * @author edgar.
 */
public class TransactionalRequest implements Route.Filter {

  private final ServiceKey<Database> key;

  private boolean enabledByDefault = true;

  /**
   * Creates a transactional request.
   *
   * <p>This constructor should be used only if you have multiple Ebean installations.
   *
   * @param name Ebean service name.
   */
  public TransactionalRequest(@NonNull String name) {
    key = ServiceKey.key(Database.class, name);
  }

  /** Creates a transactional request. */
  public TransactionalRequest() {
    key = ServiceKey.key(Database.class);
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

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      if (ctx.getRoute().isTransactional(enabledByDefault)) {
        var db = ctx.require(key);
        try (var transaction = db.beginTransaction()) {
          var result = next.apply(ctx);
          transaction.commit();
          return result;
        }
      } else {
        return next.apply(ctx);
      }
    };
  }
}
