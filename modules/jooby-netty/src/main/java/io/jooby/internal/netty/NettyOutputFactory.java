/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ResourceLeakDetector;

public record NettyOutputFactory(ByteBufAllocator allocator) implements OutputFactory {
  private static final String LEAK_DETECTION = "io.netty.leakDetection.level";

  static {
    System.setProperty(
        LEAK_DETECTION,
        System.getProperty(LEAK_DETECTION, ResourceLeakDetector.Level.DISABLED.name()));
    ResourceLeakDetector.setLevel(
        ResourceLeakDetector.Level.valueOf(System.getProperty(LEAK_DETECTION)));
  }

  /**
   * Create a new {@code OutputFactory} based on the given factory.
   *
   * @param allocator the factory to use
   * @see io.netty.buffer.PooledByteBufAllocator
   * @see io.netty.buffer.UnpooledByteBufAllocator
   */
  public NettyOutputFactory {}

  @Override
  @NonNull public Output newBufferedOutput(int size) {
    return new NettyBufferedOutput(this.allocator.buffer(size));
  }

  @Override
  @NonNull public Output wrap(ByteBuffer buffer) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(buffer));
  }

  @Override
  @NonNull public Output wrap(byte[] bytes) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(bytes));
  }

  @Override
  @NonNull public Output wrap(byte[] bytes, int offset, int length) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(bytes, offset, length));
  }

  @Override
  @NonNull public Output newCompositeOutput() {
    return new NettyBufferedOutput(allocator.compositeBuffer(48));
  }
}
