/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;

import io.jooby.Context;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NettyOutputStatic implements NettyByteBufRef {
  private final ByteBuffer buffer;
  private final NettyString contentLength;

  protected NettyOutputStatic(ByteBuffer buffer) {
    this.buffer = buffer;
    this.contentLength = new NettyString(Integer.toString(size()));
  }

  @Override
  public int size() {
    return buffer.remaining();
  }

  public ByteBuf byteBuf() {
    return Unpooled.wrappedBuffer(buffer);
  }

  @Override
  public void send(Context ctx) {
    if (ctx.getClass() == NettyContext.class) {
      ((NettyContext) ctx).send(Unpooled.wrappedBuffer(buffer), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }
}
