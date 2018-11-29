package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class DefaultHandler implements Route.Handler {

  private Route.Handler next;

  public DefaultHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    try {
      Object result = next.apply(ctx);
      if (!ctx.isResponseStarted()) {
        ctx.send(result);
      }
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }
}
