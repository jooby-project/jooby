/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class SendDirect implements LinkedHandler {
  private Route.Handler next;

  public SendDirect(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    try {
      next.apply(ctx);
      return ctx;
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
