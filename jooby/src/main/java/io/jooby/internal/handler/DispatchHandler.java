/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

public class DispatchHandler implements LinkedHandler {
  private final Route.Handler next;
  private final Executor executor;

  public DispatchHandler(Route.Handler next, Executor executor) {
    this.next = next;
    this.executor = executor;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.dispatch(executor, () -> {
      try {
        next.apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
      }
    });
  }

  @Override public Route.Handler next() {
    return next;
  }
}
