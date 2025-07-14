/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.buffer.BufferOptions;
import io.jooby.buffer.BufferedOutput;
import io.jooby.buffer.BufferedOutputFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ResourceLeakDetector;

public class NettyBufferedOutputFactory implements BufferedOutputFactory {
  private static final String LEAK_DETECTION = "io.netty.leakDetection.level";

  static {
    System.setProperty(
        LEAK_DETECTION,
        System.getProperty(LEAK_DETECTION, ResourceLeakDetector.Level.DISABLED.name()));
    ResourceLeakDetector.setLevel(
        ResourceLeakDetector.Level.valueOf(System.getProperty(LEAK_DETECTION)));
  }

  private final ByteBufAllocator allocator;
  private BufferOptions options;

  public NettyBufferedOutputFactory(ByteBufAllocator allocator, BufferOptions options) {
    this.allocator = allocator;
    this.options = options;
  }

  public ByteBufAllocator getAllocator() {
    return allocator;
  }

  @Override
  public BufferOptions getOptions() {
    return options;
  }

  @Override
  public BufferedOutputFactory setOptions(BufferOptions options) {
    this.options = options;
    return this;
  }

  @Override
  public @NonNull BufferedOutput newBufferedOutput(boolean direct, int size) {
    return new NettyBufferedOutput(
        direct ? this.allocator.directBuffer(size) : this.allocator.heapBuffer(size));
  }

  @Override
  @NonNull public BufferedOutput wrap(@NonNull ByteBuffer buffer) {
    return new NettyByteBufferWrappedOutput(buffer);
  }

  @Override
  public BufferedOutput wrap(@NonNull String value, @NonNull Charset charset) {
    return new NettyBufferedOutput(Unpooled.wrappedBuffer(value.getBytes(charset)));
  }

  @Override
  @NonNull public BufferedOutput wrap(@NonNull byte[] bytes) {
    return new NettyByteArrayWrappedOutput(bytes, 0, bytes.length);
  }

  @Override
  @NonNull public BufferedOutput wrap(@NonNull byte[] bytes, int offset, int length) {
    return new NettyByteArrayWrappedOutput(bytes, 0, bytes.length);
  }

  @Override
  @NonNull public BufferedOutput newCompositeOutput() {
    return new NettyBufferedOutput(allocator.compositeBuffer(48));
  }
}
