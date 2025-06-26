/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.output;

import java.nio.ByteBuffer;

import io.jooby.output.ChunkedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ResourceLeakDetector;

public class NettyOutputFactory implements OutputFactory {
  private static final String LEAK_DETECTION = "io.netty.leakDetection.level";

  static {
    System.setProperty(
        LEAK_DETECTION,
        System.getProperty(LEAK_DETECTION, ResourceLeakDetector.Level.DISABLED.name()));
    ResourceLeakDetector.setLevel(
        ResourceLeakDetector.Level.valueOf(System.getProperty(LEAK_DETECTION)));
  }

  private final ByteBufAllocator allocator;

  /**
   * Create a new {@code NettyDataBufferFactory} based on the given factory.
   *
   * @param allocator the factory to use
   * @see io.netty.buffer.PooledByteBufAllocator
   * @see io.netty.buffer.UnpooledByteBufAllocator
   */
  public NettyOutputFactory(ByteBufAllocator allocator) {
    this.allocator = allocator;
  }

  public NettyOutputFactory() {
    this(ByteBufAllocator.DEFAULT);
  }

  public ByteBufAllocator allocator() {
    return allocator;
  }

  @Override
  public Output newBufferedOutput(int size) {
    return new NettyBufferedOutput(this.allocator.buffer(size));
  }

  @Override
  public Output wrap(ByteBuffer buffer) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(buffer));
  }

  @Override
  public Output wrap(byte[] bytes) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(bytes));
  }

  @Override
  public Output wrap(byte[] bytes, int offset, int length) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(bytes, offset, length));
  }

  @Override
  public ChunkedOutput newChunkedOutput() {
    return new NettyChunkedOutput(allocator.compositeBuffer(48));
  }
}
