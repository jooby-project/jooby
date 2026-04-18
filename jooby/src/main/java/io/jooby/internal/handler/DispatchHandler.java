/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import java.util.concurrent.Executor;

import io.jooby.Route;

public class DispatchHandler implements Route.Filter {
  private final Executor executor;

  public DispatchHandler(Executor executor) {
    this.executor = executor;
  }

  @Override
  public Route.Handler apply(Route.Handler next) {
    return ctx ->
        ctx.dispatch(
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
  public String toString() {
    return "dispatch";
  }
}
