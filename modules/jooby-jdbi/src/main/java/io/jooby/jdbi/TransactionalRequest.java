package io.jooby.jdbi;

import io.jooby.Route;
import io.jooby.Route.Decorator;
import io.jooby.ServiceKey;
import io.jooby.RequestScope;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nonnull;

public class TransactionalRequest implements Decorator {

  private ServiceKey<Jdbi> key;

  public TransactionalRequest(@Nonnull String name) {
    key = ServiceKey.key(Jdbi.class, name);
  }

  public TransactionalRequest() {
    key = ServiceKey.key(Jdbi.class);
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      Jdbi jdbi = ctx.require(key);
      try(Handle handle = jdbi.open()) {
        RequestScope.bind(jdbi, handle);
        return handle.inTransaction(h -> next.apply(ctx));
      } finally {
        RequestScope.unbind(jdbi);
      }
    };
  }
}
