/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;

public class SendDirect implements Route.Filter {

  public static final SendDirect DIRECT = new SendDirect();

  private SendDirect() {}

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      try {
        next.apply(ctx);
        return ctx;
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }

  @Override
  public String toString() {
    return "direct";
  }
}
