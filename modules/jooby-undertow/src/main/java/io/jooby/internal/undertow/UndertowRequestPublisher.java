/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.undertow.server.HttpServerExchange;

public class UndertowRequestPublisher implements Flow.Publisher<byte[]> {
  private final HttpServerExchange exchange;

  public UndertowRequestPublisher(HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
    // We use the Subscription to manage the state between Undertow and the Subscriber
    UndertowReceiverSubscription sub = new UndertowReceiverSubscription(exchange, subscriber);
    subscriber.onSubscribe(sub);
  }
}

class UndertowReceiverSubscription implements Flow.Subscription {
  private final HttpServerExchange exchange;
  private final Flow.Subscriber<? super byte[]> subscriber;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicLong demand = new AtomicLong(0);
  private final AtomicBoolean readingStarted = new AtomicBoolean(false);

  public UndertowReceiverSubscription(
      HttpServerExchange exchange, Flow.Subscriber<? super byte[]> subscriber) {
    this.exchange = exchange;
    this.subscriber = subscriber;
  }

  @Override
  public void request(long n) {
    if (n <= 0) return;

    // Add to our demand counter
    long prevDemand = demand.getAndAdd(n);

    // Case 1: First time starting the read
    if (readingStarted.compareAndSet(false, true)) {
      startReading();
    }
    // Case 2: We were paused (demand was 0) and now have new demand
    else if (prevDemand == 0) {
      exchange.getRequestReceiver().resume();
    }
  }

  private void startReading() {
    exchange
        .getRequestReceiver()
        .receivePartialBytes(
            (exch, message, last) -> {
              if (message.length > 0) {
                // Pass bytes to De-framer
                subscriber.onNext(message);
              }
              // If we've exhausted the demand requested by the Bridge, pause Undertow
              if (demand.decrementAndGet() == 0) {
                exchange.getRequestReceiver().pause();
              }
              // THE KEY FIX:
              // 1. If 'last' is true, the stream is definitely over.
              // 2. If 'isRequestComplete' is true, Undertow's internal state knows it's over.
              if (last) {
                subscriber.onComplete();
              }
            },
            (exch, err) -> {
              subscriber.onError(err);
            });
  }

  @Override
  public void cancel() {
    exchange.getRequestReceiver().pause();
  }
}
