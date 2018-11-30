package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class DetachHandler implements ChainedHandler {
  private final Route.Handler next;

  public DetachHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.detach(() -> next.execute(ctx));
  }

  @Override public Route.Handler next() {
    return next;
  }
}
