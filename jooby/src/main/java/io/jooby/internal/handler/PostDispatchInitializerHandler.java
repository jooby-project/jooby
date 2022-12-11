/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.internal.ContextInitializer;

public class PostDispatchInitializerHandler implements Route.Filter {

  private final ContextInitializer initializer;

  public PostDispatchInitializerHandler(ContextInitializer initializer) {
    this.initializer = initializer;
  }

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      try {
        initializer.apply(ctx);
        return next.apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    };
  }
}
