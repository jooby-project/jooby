/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.jooby.output.BufferedOutput;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;
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

  private static class NettyContextOutputFactory extends NettyOutputFactory {
    public NettyContextOutputFactory(ByteBufAllocator allocator, OutputOptions options) {
      super(allocator, options);
    }

    @Override
    public Output wrap(String value, Charset charset) {
      return new NettyWrappedOutput(Unpooled.wrappedBuffer(value.getBytes(charset)));
    }

    @Override
    public Output wrap(ByteBuffer buffer) {
      return new NettyWrappedOutput(Unpooled.wrappedBuffer(buffer));
    }

    @Override
    public Output wrap(byte[] bytes) {
      return new NettyWrappedOutput(Unpooled.wrappedBuffer(bytes));
    }

    @Override
    public Output wrap(byte[] bytes, int offset, int length) {
      return new NettyWrappedOutput(Unpooled.wrappedBuffer(bytes, offset, length));
    }
  }

  private final ByteBufAllocator allocator;
  private final OutputOptions options;

  public NettyOutputFactory(ByteBufAllocator allocator, OutputOptions options) {
    this.allocator = allocator;
    this.options = options;
  }

  public ByteBufAllocator getAllocator() {
    return allocator;
  }

  @Override
  public OutputOptions getOptions() {
    return options;
  }

  @Override
  public BufferedOutput allocate(boolean direct, int size) {
    return new NettyByteBufOutput(
        direct ? this.allocator.directBuffer(size) : this.allocator.heapBuffer(size));
  }

  @Override
  public Output wrap(ByteBuffer buffer) {
    return new NettyOutputStatic(buffer);
  }

  @Override
  public Output wrap(byte[] bytes) {
    return wrap(bytes, 0, bytes.length);
  }

  @Override
  public Output wrap(byte[] bytes, int offset, int length) {
    return new NettyOutputUnsafeHeapByteBuf(bytes, offset, length);
  }

  @Override
  public BufferedOutput newComposite() {
    return new NettyByteBufOutput(allocator.compositeBuffer(48));
  }

  @Override
  public OutputFactory getContextFactory() {
    return new NettyContextOutputFactory(allocator, options);
  }
}
