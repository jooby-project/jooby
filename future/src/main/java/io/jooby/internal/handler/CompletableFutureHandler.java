package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureHandler implements Route.Handler {

  private final Route.Handler next;

  public CompletableFutureHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      CompletableFuture result = (CompletableFuture) next.apply(ctx);
      return result.whenComplete((value, x) -> {
        try {
          if (x != null) {
            ctx.sendError((Throwable) x);
          } else {
            ctx.send(value);
          }
        } catch (Throwable newx) {
          ctx.sendError(newx);
        }
      });
    } catch (Throwable x) {
      ctx.sendError(x);
      return CompletableFuture.failedFuture(x);
    }
  }
}
