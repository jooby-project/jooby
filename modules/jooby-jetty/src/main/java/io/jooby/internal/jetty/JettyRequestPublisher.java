/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import java.util.HexFormat;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyRequestPublisher implements Flow.Publisher<byte[]> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Request request;

  public JettyRequestPublisher(Request request) {
    this.request = request;
  }

  @Override
  public void subscribe(Flow.Subscriber<? super byte[]> subscriber) {
    var subscription = new JettySubscription(request, subscriber);
    subscriber.onSubscribe(subscription);
  }
}

/**
 * Professional Jetty 12 Core Subscription. Uses the demand-callback pattern to satisfy gRPC stream
 * requirements.
 */
class JettySubscription implements Flow.Subscription {

  private static final Logger log = LoggerFactory.getLogger(JettySubscription.class);
  private final Request request;
  private final Flow.Subscriber<? super byte[]> subscriber;

  private final AtomicLong demand = new AtomicLong();
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final AtomicBoolean completed = new AtomicBoolean(false);

  public JettySubscription(Request request, Flow.Subscriber<? super byte[]> subscriber) {
    this.request = request;
    this.subscriber = subscriber;
  }

  private final AtomicBoolean demandPending = new AtomicBoolean(false);

  private void process(String call) {
    log.info("{}- start reading request", call);
    try {
      var demandMore = false;
      while (true) {
        // 2. Check for data. We MUST read if the deframer is "hungry,"
        // even if application demand is 0.
        var chunk = request.read();

        if (chunk == null) {
          log.info("{}- demanding more", call);
          request.demand(
              () -> {
                process(call + ".demand");
              });
          return;
        }

        if (Content.Chunk.isFailure(chunk)) {
          log.info("{}- bad chunk: {}", call, chunk);
          boolean fatal = chunk.isLast();
          if (fatal) {
            handleComplete();
            return;
          } else {
            handleError(chunk.getFailure());
            return;
          }
        }
        var buffer = chunk.getByteBuffer();

        if (buffer != null && buffer.hasRemaining()) {
          byte[] bytes = new byte[buffer.remaining()];
          buffer.get(bytes);

          log.info("{}- byte read: {}", call, HexFormat.of().formatHex(bytes));
          //          demand.decrementAndGet();
          subscriber.onNext(bytes);
        }
        chunk.release();

        if (chunk.isLast()) {
          log.info("{}- last reach", call);
          // Even if we have 0 demand, we must finish the stream
          handleComplete();
          return;
        }
      }
    } catch (Throwable t) {
      handleError(t);
    } finally {
      log.info("{}- finish reading request", call);
    }
  }

  private void handleComplete() {
    if (completed.compareAndSet(false, true) && !cancelled.get()) {
      log.info("handle complete");
      subscriber.onComplete();
    }
  }

  private void handleError(Throwable t) {
    if (completed.compareAndSet(false, true) && !cancelled.get()) {
      log.info("handle error", t);
      subscriber.onError(t);
    }
  }

  long c = 0;

  @Override
  public void request(long n) {
    if (n <= 0) return;
    log.info("init request({})", n);
    c += n;
    process(Long.toString(c));
  }

  @Override
  public void cancel() {
    cancelled.set(true);
  }
}
