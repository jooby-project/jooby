/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CompletionStageHandler implements LinkedHandler {

  private final Route.Handler next;

  public CompletionStageHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      CompletionStage result = (CompletionStage) next.apply(ctx);
      return result.whenComplete((value, x) -> {
        try {
          if (x != null) {
            ctx.sendError((Throwable) x);
          } else {
            ctx.render(value);
          }
        } catch (Throwable newx) {
          ctx.sendError(newx);
        }
      });
    } catch (Throwable x) {
      ctx.sendError(x);
      return CompletableFuture.completedFuture(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
