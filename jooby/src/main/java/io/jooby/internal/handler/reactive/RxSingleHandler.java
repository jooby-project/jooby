/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.internal.handler.LinkedHandler;
import io.reactivex.Single;

import javax.annotation.Nonnull;

public class RxSingleHandler implements LinkedHandler {

  private final Route.Handler next;

  public RxSingleHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Single result = (Single) next.apply(ctx);
      result.subscribe(new RxSubscriber(ctx));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Single.error(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
