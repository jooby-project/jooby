/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.netty.buffer.ByteBuf;

public class NettyOutputByteArrayStatic implements NettyByteBufRef {
  private final byte[] bytes;
  private final int offset;
  private final int length;
  private final NettyString contentLength;

  protected NettyOutputByteArrayStatic(byte[] bytes, int offset, int length) {
    this.bytes = bytes;
    this.offset = offset;
    this.length = length;
    this.contentLength = new NettyString(Integer.toString(size()));
  }

  @Override
  public int size() {
    return length - offset;
  }

  @NonNull public ByteBuf byteBuf() {
    return wrappedBuffer(bytes, offset, length);
  }

  @Override
  public void send(Context ctx) {
    if (ctx.getClass() == NettyContext.class) {
      ((NettyContext) ctx).send(wrappedBuffer(bytes, offset, length), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }
}
