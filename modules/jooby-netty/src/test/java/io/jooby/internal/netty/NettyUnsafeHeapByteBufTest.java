/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NettyUnsafeHeapByteBufTest {

  @Test
  void shouldAllocateEmptyArrayWhenInitialCapacityIsZero() {
    // Triggers the `if (initialCapacity == 0)` branch in allocateArray
    NettyUnsafeHeapByteBuf buf = new NettyUnsafeHeapByteBuf(0, 10);

    assertEquals(0, buf.capacity());
    assertEquals(0, buf.array().length);
  }

  @Test
  void shouldAllocateStandardArrayWhenInitialCapacityIsGreaterThanZero() {
    // Triggers the `super.allocateArray(initialCapacity)` branch
    NettyUnsafeHeapByteBuf buf = new NettyUnsafeHeapByteBuf(10, 100);

    assertEquals(10, buf.capacity());
    assertEquals(10, buf.array().length);
  }

  @Test
  void shouldBypassRetain() {
    NettyUnsafeHeapByteBuf buf = new NettyUnsafeHeapByteBuf(10, 10);

    // Both retain signatures should simply return 'this' without tracking
    assertSame(buf, buf.retain());
    assertSame(buf, buf.retain(5));
  }

  @Test
  void shouldBypassTouch() {
    NettyUnsafeHeapByteBuf buf = new NettyUnsafeHeapByteBuf(10, 10);

    // Both touch signatures should simply return 'this' without tracking
    assertSame(buf, buf.touch());
    assertSame(buf, buf.touch("My Hint"));
  }

  @Test
  void shouldBypassRelease() {
    NettyUnsafeHeapByteBuf buf = new NettyUnsafeHeapByteBuf(10, 10);

    // Both release signatures should return false indicating it's un-releasable
    assertFalse(buf.release());
    assertFalse(buf.release(3));
  }
}
