/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.internal.handler.LinkedHandler;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class ReactorMonoHandler implements LinkedHandler {

  private final Route.Handler next;

  public ReactorMonoHandler(Route.Handler next) {
    this.next = next;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Mono result = (Mono) next.apply(ctx);
      result.subscribe(ctx::render, x -> ctx.sendError((Throwable) x));
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return Mono.error(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
