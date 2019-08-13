/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.internal.handler.LinkedHandler;

import javax.annotation.Nonnull;

public class HeadResponseHandler implements LinkedHandler {
  private Route.Handler next;

  public HeadResponseHandler(Route.Handler next) {
    this.next = next;
  }

  @Override public Route.Handler next() {
    return next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    return ctx.getMethod().equals(Router.HEAD)
        ? next.apply(new HeadContext(ctx))
        : next.apply(ctx);
  }
}
