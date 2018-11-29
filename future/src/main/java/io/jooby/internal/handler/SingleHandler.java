package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.reactivex.Single;

import javax.annotation.Nonnull;

public class SingleHandler implements Route.Handler {

  private final Route.Handler next;

  public SingleHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Single result = (Single) next.apply(ctx);
      result.subscribe(ctx::send, x -> ctx.sendError((Throwable) x));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Single.error(x);
    }
  }
}
