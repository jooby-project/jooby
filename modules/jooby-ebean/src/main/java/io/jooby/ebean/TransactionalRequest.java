package io.jooby.ebean;

import io.ebean.Database;
import io.ebean.Transaction;
import io.jooby.Route;
import io.jooby.ServiceKey;

import javax.annotation.Nonnull;

/**
 * Start a new transaction on each incoming request. Its commit the transaction is no exception is
 * thrown or rollback in case of an exception.
 *
 * @author edgar.
 */
public class TransactionalRequest implements Route.Decorator {

  private ServiceKey<Database> key;

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

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      Database db = ctx.require(key);
      try (Transaction transaction = db.beginTransaction()) {
        Object result = next.apply(ctx);
        transaction.commit();
        return result;
      }
    };
  }
}
