/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;

public class UndertowGrpcInputBridge
    implements Flow.Subscription, ChannelListener<StreamSourceChannel> {

  private final HttpServerExchange exchange;
  private final Flow.Subscriber<ByteBuffer> subscriber;
  private final AtomicLong demand = new AtomicLong();
  private StreamSourceChannel channel;

  private final ByteBuffer buffer = ByteBuffer.allocate(8192);

  public UndertowGrpcInputBridge(
      HttpServerExchange exchange, Flow.Subscriber<ByteBuffer> subscriber) {
    this.exchange = exchange;
    this.subscriber = subscriber;
  }

  public void start() {
    this.channel = exchange.getRequestChannel();
    this.channel.getReadSetter().set(this);
    subscriber.onSubscribe(this);
  }

  @Override
  public void request(long n) {
    if (n <= 0) {
      subscriber.onError(new IllegalArgumentException("Demand must be positive"));
      return;
    }

    if (demand.getAndAdd(n) == 0 && channel != null) {
      // CRITICAL FIX: wakeupReads() forces the listener to fire immediately,
      // draining any data that arrived in the same packet as the headers.
      channel.wakeupReads();
    }
  }

  @Override
  public void cancel() {
    demand.set(0);
    IoUtils.safeClose(channel);
  }

  @Override
  public void handleEvent(StreamSourceChannel channel) {
    try {
      while (demand.get() > 0) {
        buffer.clear();
        int res = channel.read(buffer);

        if (res == -1) {
          channel.suspendReads();
          subscriber.onComplete();
          return;
        } else if (res == 0) {
          // Buffer drained, waiting for more data from the network
          return;
        }

        buffer.flip();
        ByteBuffer chunk = ByteBuffer.allocate(buffer.remaining());
        chunk.put(buffer);
        chunk.flip();

        subscriber.onNext(chunk);
        demand.decrementAndGet();
      }

      if (demand.get() == 0) {
        channel.suspendReads();
      }

    } catch (Throwable t) {
      subscriber.onError(t);
      IoUtils.safeClose(channel);
    }
  }
}
