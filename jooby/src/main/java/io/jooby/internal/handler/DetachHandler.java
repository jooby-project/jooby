/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;

public class DetachHandler implements LinkedHandler {
  private final Route.Handler next;

  public DetachHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return ctx.detach(() -> {
      try {
        next.apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
      }
    });
  }

  @Override public Route.Handler next() {
    return next;
  }
}
