package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class DefaultHandler implements ChainedHandler {

  private Route.Handler next;

  public DefaultHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
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

  @Override public Route.Handler next() {
    return next;
  }
}
