/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import kotlinx.coroutines.Deferred;
import kotlinx.coroutines.Job;

public class KotlinJobHandler implements LinkedHandler {
  private final Route.Handler next;

  public KotlinJobHandler(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    try {
      Object result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        return result;
      }
      ((Job) result)
          .invokeOnCompletion(
              x -> {
                if (x != null) {
                  ctx.sendError(x);
                } else {
                  if (result instanceof Deferred) {
                    ctx.render(((Deferred) result).getCompleted());
                  }
                }
                return null;
              });
      return ctx;
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
