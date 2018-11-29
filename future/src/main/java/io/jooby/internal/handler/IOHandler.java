package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class IOHandler implements Route.Handler {
  private final Route.Handler next;

  public IOHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.dispatch(() -> next.execute(ctx));
  }
}
