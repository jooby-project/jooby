/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;

public class DefaultHandler implements Route.Filter {

  public static final DefaultHandler DEFAULT = new DefaultHandler();

  private DefaultHandler() {}

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
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
    };
  }
}
