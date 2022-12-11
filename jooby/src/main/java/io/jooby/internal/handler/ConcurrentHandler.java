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
      } else if (result instanceof CompletionStage future) {
        return future.whenComplete(
            (value, x) -> {
              try {
                if (x != null) {
                  Throwable exception = (Throwable) x;
                  if (exception instanceof CompletionException) {
                    exception = Optional.ofNullable(exception.getCause()).orElse(exception);
                  }
                  ctx.sendError(exception);
                } else {
                  ctx.render(value);
                }
              } catch (Throwable cause) {
                ctx.sendError(cause);
              }
            });
      }
      return result;
    };
  }

  @Override
  public void setRoute(Route route) {
    route.setReactive(true);
  }
}
