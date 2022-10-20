/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;

public class DefaultHandler implements LinkedHandler {

  private Route.Handler next;

  public DefaultHandler(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    try {
      Object result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        return result;
      }
      ctx.render(result);
      return result;
    } catch (Throwable x) {
      ctx.sendError(x);
      return x;
    }
  }

  @Override
  public Route.Handler next() {
    return next;
  }
}
