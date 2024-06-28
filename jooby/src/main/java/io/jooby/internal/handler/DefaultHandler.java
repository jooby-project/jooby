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
        Object value = next.apply(ctx);
        if (value != ctx && !ctx.isResponseStarted()) {
          ctx.render(value);
        }
        return value;
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }

  @Override
  public String toString() {
    return "default";
  }
}
