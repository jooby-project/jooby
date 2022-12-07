/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.handler.ChunkedSubscriber;

/**
 * Utility function for handling {@link CompletionStage} and {@link Flow.Publisher}.
 *
 * @author edgar
 * @since 3.0.0
 */
public class ReactiveSupport {

  /**
   * Creates a subscriber from web context.
   *
   * @param ctx Web Context.
   * @return New subscriber.
   * @param <T> Flow type.
   */
  public static <T> Flow.Subscriber<T> newSubscriber(Context ctx) {
    return new ChunkedSubscriber(ctx);
  }

  /**
   * Flow publisher filter. Handle flow responses.
   *
   * @return Filter.
   */
  public static Route.Filter flow() {
    return new Route.Filter() {
      @NonNull @Override
      public Route.Handler apply(@NonNull Route.Handler next) {
        return ctx -> {
          Object result = next.apply(ctx);
          if (result instanceof Flow.Publisher publisher) {
            publisher.subscribe(newSubscriber(ctx));
          }
          return result;
        };
      }

      @Override
      public void setRoute(Route route) {
        route.setReactive(true);
      }
    };
  }

  /**
   * Completable future filter. Handle completable future responses.
   *
   * @return Filter.
   */
  public static Route.Filter completableFuture() {
    return new Route.Filter() {
      @NonNull @Override
      public Route.Handler apply(@NonNull Route.Handler next) {
        return ctx -> {
          Object result = next.apply(ctx);
          if (result instanceof CompletionStage future) {
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
    };
  }
}
