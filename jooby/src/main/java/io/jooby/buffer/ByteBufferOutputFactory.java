/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.buffer.ByteBufferOutput;
import io.jooby.internal.buffer.ByteBufferWrappedOutput;
import io.jooby.internal.buffer.CompsiteByteBufferOutput;

/**
 * An output factory backed by {@link ByteBuffer}.
 *
 * @author edgar
 * @since 4.0.0
 */
public class ByteBufferOutputFactory implements BufferedOutputFactory {
  private BufferOptions options;

  public ByteBufferOutputFactory(BufferOptions options) {
    this.options = options;
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
  public BufferedOutput newBufferedOutput(boolean direct, int size) {
    return new ByteBufferOutput(direct, size);
  }

  @Override
  public BufferedOutput newCompositeOutput() {
    return new CompsiteByteBufferOutput();
  }

  @Override
  public BufferedOutput wrap(@NonNull ByteBuffer buffer) {
    return new ByteBufferWrappedOutput(buffer);
  }

  @Override
  public BufferedOutput wrap(@NonNull byte[] bytes) {
    return new ByteBufferWrappedOutput(ByteBuffer.wrap(bytes));
  }

  @Override
  public BufferedOutput wrap(@NonNull byte[] bytes, int offset, int length) {
    return new ByteBufferWrappedOutput(ByteBuffer.wrap(bytes, offset, length));
  }
}
