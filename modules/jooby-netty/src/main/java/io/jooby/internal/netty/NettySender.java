/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.internal.netty.NettyByteBufRef.byteBuf;

import io.jooby.Sender;
import io.jooby.output.Output;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class NettySender implements Sender {

  private final NettyContext ctx;
  private final ChannelHandlerContext context;

  public NettySender(NettyContext ctx) {
    this.ctx = ctx;
    this.context = ctx.ctx;
  }

  @Override
  public Sender write(byte[] data, Callback callback) {
    context
        .writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data)))
        .addListener(newChannelFutureListener(ctx, callback));
    return this;
  }

  @Override
  public Sender write(Output output, Callback callback) {
    context
        .writeAndFlush(new DefaultHttpContent(byteBuf(output)))
        .addListener(newChannelFutureListener(ctx, callback));
    return this;
  }

  @Override
  public void close() {
    context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, ctx.promise());
    ctx.requestComplete();
  }

  private static ChannelFutureListener newChannelFutureListener(
      NettyContext ctx, Callback callback) {
    return future -> {
      if (future.isSuccess()) {
        callback.onComplete(ctx, null);
      } else {
        Throwable cause = future.cause();
        try {
          callback.onComplete(ctx, cause);
        } finally {
          ctx.log(cause);
        }
      }
    };
  }
}
