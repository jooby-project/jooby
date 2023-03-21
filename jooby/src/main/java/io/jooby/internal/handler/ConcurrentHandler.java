/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import static io.jooby.ReactiveSupport.newSubscriber;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;

public class ConcurrentHandler implements Route.Filter {

  @NonNull @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      Object result = next.apply(ctx);
      if (result instanceof Flow.Publisher publisher) {
        publisher.subscribe(newSubscriber(ctx));
        // Return context to mark as handled
        return ctx;
      } else if (result instanceof CompletionStage future) {
        future.whenComplete(
            (value, x) -> {
              try {
                Route.After after = ctx.getRoute().getAfter();
                if (after != null) {
                  // run after:
                  after.apply(ctx, value, unwrap((Throwable) x));
                }
                if (x != null) {
                  Throwable exception = unwrap((Throwable) x);
                  ctx.sendError(exception);
                } else {
                  ctx.render(value);
                }
              } catch (Throwable cause) {
                ctx.sendError(cause);
              }
            });
        // Return context to mark as handled
        return ctx;
      }
      return result;
    };
  }

  private Throwable unwrap(Throwable x) {
    if (x instanceof CompletionException) {
      return Optional.ofNullable(x.getCause()).orElse(x);
    } else {
      return x;
    }
  }

  @Override
  public void setRoute(Route route) {
    route.setReactive(true);
  }

  @Override
  public String toString() {
    return "concurrent";
  }
}
