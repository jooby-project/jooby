package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class WorkerHandler implements ChainedHandler {
  private final Route.Handler next;

  public WorkerHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.dispatch(() -> next.execute(ctx));
  }

  @Override public Route.Handler next() {
    return next;
  }
}
