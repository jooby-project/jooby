/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import io.jooby.internal.handler.ChunkedSubscriber;
import io.jooby.internal.handler.ConcurrentHandler;

/**
 * Utility function for handling {@link CompletionStage} and {@link Flow.Publisher}.
 *
 * @author edgar
 * @since 3.0.0
 */
public class ReactiveSupport {

  private static final Route.Filter CONCURRENT = new ConcurrentHandler();

  /**
   * Creates a subscriber from web context.
   *
   * @param ctx Web Context.
   * @param <T> Flow type.
   * @return New subscriber.
   */
  public static <T> Flow.Subscriber<T> newSubscriber(Context ctx) {
    return new ChunkedSubscriber(ctx);
  }

  /**
   * Concurrent filter. Handle {@link CompletionStage} and {@link Flow} responses.
   *
   * @return Filter.
   */
  public static Route.Filter concurrent() {
    return CONCURRENT;
  }
}
