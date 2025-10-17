/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.netty.buffer.ByteBuf;

public class NettyOutputUnsafeHeapByteBuf implements NettyByteBufRef {
  private final ByteBuf buf;
  private final NettyString contentLength;
  private final int length;

  protected NettyOutputUnsafeHeapByteBuf(byte[] bytes, int offset, int len) {
    this.length = len - offset;
    this.buf = new NettyUnsafeHeapByteBuf(this.length, this.length);
    this.buf.writeBytes(bytes, offset, length);
    this.contentLength = new NettyString(Integer.toString(size()));
  }

  @Override
  public int size() {
    return length;
  }

  @NonNull public ByteBuf byteBuf() {
    return buf.slice(0, length);
  }

  @Override
  public void send(Context ctx) {
    if (ctx.getClass() == NettyContext.class) {
      ((NettyContext) ctx).send(buf.slice(0, length), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }
}
