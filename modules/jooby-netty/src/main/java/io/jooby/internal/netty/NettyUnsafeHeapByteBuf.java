/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledUnsafeHeapByteBuf;

/** An un-releasable, un-pooled, un-instrumented, un-safe heap {@code ByteBuf}. */
public final class NettyUnsafeHeapByteBuf extends UnpooledUnsafeHeapByteBuf {

  private static final byte[] EMPTY = new byte[0];

  public NettyUnsafeHeapByteBuf(int initialCapacity, int maxCapacity) {
    super(UnpooledByteBufAllocator.DEFAULT, initialCapacity, maxCapacity);
  }

  @Override
  protected byte[] allocateArray(int initialCapacity) {
    if (initialCapacity == 0) {
      return EMPTY;
    }
    return super.allocateArray(initialCapacity);
  }

  @Override
  public ByteBuf retain(int increment) {
    return this;
  }

  @Override
  public ByteBuf retain() {
    return this;
  }

  @Override
  public ByteBuf touch() {
    return this;
  }

  @Override
  public ByteBuf touch(Object hint) {
    return this;
  }

  @Override
  public boolean release() {
    return false;
  }

  @Override
  public boolean release(int decrement) {
    return false;
  }
}
