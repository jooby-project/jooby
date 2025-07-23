/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.netty.buffer.ByteBuf;

public class NettyOutputStatic implements NettyByteBufRef {
  private final Supplier<ByteBuf> provider;
  private final int size;
  private final NettyString contentLength;

  protected NettyOutputStatic(int length, Supplier<ByteBuf> provider) {
    this.provider = provider;
    this.size = length;
    this.contentLength = new NettyString(Integer.toString(length));
  }

  @Override
  public int size() {
    return size;
  }

  @NonNull public ByteBuf byteBuf() {
    return provider.get();
  }

  @Override
  public void send(Context ctx) {
    if (ctx.getClass() == NettyContext.class) {
      ((NettyContext) ctx).send(provider.get(), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }
}
