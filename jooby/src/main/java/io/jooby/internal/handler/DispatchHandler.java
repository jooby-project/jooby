/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import java.util.concurrent.Executor;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;

public class DispatchHandler implements LinkedHandler {
  private final Route.Handler next;
  private final Executor executor;

  public DispatchHandler(Route.Handler next, Executor executor) {
    this.next = next;
    this.executor = executor;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    return ctx.dispatch(
        executor,
        () -> {
          try {
            next.apply(ctx);
          } catch (Throwable x) {
            ctx.sendError(x);
          }
        });
  }

  @Override
  public Route.Handler next() {
    return next;
  }
}
