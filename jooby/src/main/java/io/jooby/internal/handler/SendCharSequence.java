/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import edu.umd.cs.findbugs.annotations.NonNull;

public class SendCharSequence implements LinkedHandler {
  private Route.Handler next;

  public SendCharSequence(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) {
    try {
      Object result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        return result;
      }
      return ctx.send(result.toString());
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
