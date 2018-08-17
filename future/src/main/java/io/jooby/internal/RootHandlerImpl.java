package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class RootHandlerImpl implements Route.RootHandler {
  private final Route.Handler next;

  public RootHandlerImpl(Route.Handler next) {
    this.next = next;
  }

  @Override public void apply(@Nonnull Context ctx) {
    try {
      next.apply(ctx);
    } catch (Throwable x) {
      ctx.sendError(x);
    } finally {
      if (ctx.isResponseStarted()) {
        ctx.destroy();
      }
    }
  }
}
