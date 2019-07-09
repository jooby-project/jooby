/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jdbi;

import io.jooby.Route;
import io.jooby.Route.Decorator;
import io.jooby.ServiceKey;
import io.jooby.RequestScope;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nonnull;

/**
 * Attach {@link Handle} to the current request. The route pipeline runs inside a transaction
 * which is commit on success or rollback in case of exception.
 *
 * Once route pipeline is executed the handle is detached from current request and closed it.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 *   install(new JdbiModule());
 *
 *   decorator(new TransactionalRequest());
 *
 *   get("/handle", ctx -> {
 *     Handle handle = require(Handle.class);
 *     // work with handle.
 *   });
 * }
 * }</pre>
 *
 * The {@link Handle} should NOT be closed it by application code.
 *
 * SQL Objects example:
 *
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 *   install(new JdbiModule()
 *     .sqlObjects(UserDAO.class)
 *   );
 *
 *   decorator(new TransactionalRequest());
 *
 *   get("/handle", ctx -> {
 *     UserDAO dao = require(UserDAO.class);
 *     // work with user dao
 *   });
 * }
 * }</pre>
 *
 * The <code>UserDAO</code> sql object is attached to the current/request attached {@link Handle}.
 *
 * @author edgar
 * @since 2.0.0
 */
public class TransactionalRequest implements Decorator {

  private ServiceKey<Jdbi> key;

  /**
   * Creates a transactional request. A jdbi with the given name must be available in the service
   * registry.
   *
   * This constructor should be used only if you have multiple Jdbi installations.
   *
   * @param name Jdbi service name.
   */
  public TransactionalRequest(@Nonnull String name) {
    key = ServiceKey.key(Jdbi.class, name);
  }

  /**
   * Creates a transactional request.
   */
  public TransactionalRequest() {
    key = ServiceKey.key(Jdbi.class);
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      Jdbi jdbi = ctx.require(key);
      try (Handle handle = jdbi.open()) {
        RequestScope.bind(jdbi, handle);
        return handle.inTransaction(h -> next.apply(ctx));
      } finally {
        RequestScope.unbind(jdbi);
      }
    };
  }
}
