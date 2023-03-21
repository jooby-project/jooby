/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;

public class WorkerHandler implements Route.Filter {
  public static final Route.Filter WORKER = new WorkerHandler();

  private WorkerHandler() {}

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx ->
        ctx.dispatch(
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
    return "worker";
  }
}
