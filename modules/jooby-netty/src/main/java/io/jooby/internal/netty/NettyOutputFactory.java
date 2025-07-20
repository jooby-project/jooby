/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
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

  private final ByteBufAllocator allocator;
  private OutputOptions options;

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
  public OutputFactory setOptions(OutputOptions options) {
    this.options = options;
    return this;
  }

  @Override
  public @NonNull Output newOutput(boolean direct, int size) {
    return new NettyOutputDefault(
        direct ? this.allocator.directBuffer(size) : this.allocator.heapBuffer(size));
  }

  @Override
  @NonNull public Output wrap(@NonNull ByteBuffer buffer) {
    return new NettyOutputStatic(buffer.remaining(), () -> Unpooled.wrappedBuffer(buffer));
  }

  @Override
  public Output wrap(@NonNull String value, @NonNull Charset charset) {
    return new NettyOutputDefault(Unpooled.wrappedBuffer(value.getBytes(charset)));
  }

  @Override
  @NonNull public Output wrap(@NonNull byte[] bytes) {
    return new NettyOutputStatic(bytes.length, () -> Unpooled.wrappedBuffer(bytes));
  }

  @Override
  @NonNull public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return new NettyOutputStatic(
        length - offset, () -> Unpooled.wrappedBuffer(bytes, offset, length));
  }

  @Override
  @NonNull public Output newCompositeOutput() {
    return new NettyOutputDefault(allocator.compositeBuffer(48));
  }
}
