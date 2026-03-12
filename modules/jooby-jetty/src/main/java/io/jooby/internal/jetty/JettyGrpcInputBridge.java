/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;

public class JettyGrpcInputBridge implements Flow.Subscription, Runnable {

  private final Request request;
  private final Flow.Subscriber<ByteBuffer> subscriber;
  private final Callback callback;
  private final AtomicLong demand = new AtomicLong();

  public JettyGrpcInputBridge(
      Request request, Flow.Subscriber<ByteBuffer> subscriber, Callback callback) {
    this.request = request;
    this.subscriber = subscriber;
    this.callback = callback;
  }

  public void start() {
    subscriber.onSubscribe(this);
  }

  @Override
  public void request(long n) {
    if (n <= 0) {
      subscriber.onError(new IllegalArgumentException("Demand must be positive"));
      return;
    }

    if (demand.getAndAdd(n) == 0) {
      run();
    }
  }

  @Override
  public void cancel() {
    demand.set(0);
    callback.failed(new CancellationException("gRPC stream cancelled by client"));
  }

  @Override
  public void run() {
    try {
      while (demand.get() > 0) {
        var chunk = request.read();

        if (chunk == null) {
          request.demand(this);
          return;
        }

        try {
          var failure = chunk.getFailure();
          if (failure != null) {
            subscriber.onError(failure);
            callback.failed(failure);
            return;
          }

          var buffer = chunk.getByteBuffer();
          if (buffer != null && buffer.hasRemaining()) {
            subscriber.onNext(buffer);
            demand.decrementAndGet();
          }

          if (chunk.isLast()) {
            subscriber.onComplete();
            return;
          }

        } finally {
          chunk.release();
        }
      }
    } catch (Throwable t) {
      subscriber.onError(t);
      callback.failed(t);
    }
  }
}
