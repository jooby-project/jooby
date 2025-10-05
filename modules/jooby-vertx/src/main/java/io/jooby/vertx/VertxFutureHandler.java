/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class VertxFutureHandler implements Route.Filter {
  @Override
  public @NonNull Route.Handler apply(Route.Handler next) {
    return ctx -> {
      var result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        // Return context to mark as handled
        return ctx;
      } else if (result instanceof Promise<?> promise) {
        return futureResult(ctx, promise.future());
      } else if (result instanceof Future<?> future) {
        return futureResult(ctx, future);
      }
      return result;
    };
  }

  private static Context futureResult(Context ctx, Future<?> future) {
    future.onComplete(
        ar -> {
          if (ar.succeeded()) {
            ctx.render(ar.result());
          } else {
            ctx.sendError(ar.cause());
          }
        });
    return ctx;
  }
}
