/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jdbi;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.RequestScope;
import io.jooby.Route;
import io.jooby.Route.Filter;
import io.jooby.ServiceKey;
import io.jooby.annotation.Transactional;

/**
 * Attach {@link Handle} to the current request. The route pipeline runs inside a transaction which
 * is commit on success or rollback in case of exception.
 *
 * <p>Once route pipeline is executed the handle is detached from current request and closed it.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new HikariModule());
 *
 *   install(new JdbiModule());
 *
 *   use(new TransactionalRequest());
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
 * <p>SQL Objects example:
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
 *   use(new TransactionalRequest());
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
public class TransactionalRequest implements Filter {

  private ServiceKey<Jdbi> key;

  private boolean enabledByDefault = true;

  /**
   * Creates a transactional request. A jdbi with the given name must be available in the service
   * registry.
   *
   * <p>This constructor should be used only if you have multiple Jdbi installations.
   *
   * @param name Jdbi service name.
   */
  public TransactionalRequest(@NonNull String name) {
    key = ServiceKey.key(Jdbi.class, name);
  }

  /** Creates a transactional request. */
  public TransactionalRequest() {
    key = ServiceKey.key(Jdbi.class);
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
        Jdbi jdbi = ctx.require(key);
        try (Handle handle = jdbi.open()) {
          RequestScope.bind(jdbi, handle);
          return handle.inTransaction(h -> next.apply(ctx));
        } finally {
          RequestScope.unbind(jdbi);
        }
      } else {
        return next.apply(ctx);
      }
    };
  }
}
