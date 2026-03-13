/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyGrpcInputBridge implements Flow.Subscription {

  private final ChannelHandlerContext ctx;
  private final Flow.Subscriber<ByteBuffer> subscriber;
  private final AtomicLong demand = new AtomicLong();

  public NettyGrpcInputBridge(ChannelHandlerContext ctx, Flow.Subscriber<ByteBuffer> subscriber) {
    this.ctx = ctx;
    this.subscriber = subscriber;
  }

  public void start() {
    // Disable auto-read. We will manually request reads based on gRPC demand.
    ctx.channel().config().setAutoRead(false);
    subscriber.onSubscribe(this);
  }

  @Override
  public void request(long n) {
    if (n <= 0) {
      subscriber.onError(new IllegalArgumentException("Demand must be positive"));
      return;
    }

    if (demand.getAndAdd(n) == 0) {
      // We transitioned from 0 to n demand, trigger a read from the socket
      ctx.read();
    }
  }

  @Override
  public void cancel() {
    demand.set(0);
    ctx.close(); // Abort the connection
  }

  public void onChunk(HttpContent chunk) {
    try {
      ByteBuf content = chunk.content();
      if (content.isReadable()) {

        // Copy the bytes. content.nioBuffer() shares memory with the ByteBuf,
        // which gets released in the finally block of NettyGrpcHandler.
        // We must copy it to an unmanaged heap buffer to prevent use-after-free corruption.
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // If onNext synchronously calls request(1), that request() will see demand transition
        // from 0 to 1 and safely trigger ctx.read().
        long currentDemand = demand.decrementAndGet();

        // Pass the isolated buffer to the gRPC deframer
        subscriber.onNext(buffer);

        if (currentDemand > 0) {
          // Still have demand from previous requests, ask Netty for the next chunk
          ctx.read();
        }
      }

      if (chunk instanceof LastHttpContent) {
        subscriber.onComplete();
      } else if (demand.get() > 0 && !content.isReadable()) {
        // Edge case: Empty chunk but not LastHttpContent, read next
        ctx.read();
      }
    } catch (Throwable t) {
      subscriber.onError(t);
      ctx.close();
    }
  }
}
