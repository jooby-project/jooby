/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.output.ByteBufferWrappedOutput;
import io.jooby.internal.output.CompsiteByteBufferOutput;

/**
 * An output factory backed by {@link ByteBuffer}.
 *
 * @author edgar
 * @since 4.0.0
 */
public class ByteBufferOutputFactory implements OutputFactory {
  private OutputOptions options;

  public ByteBufferOutputFactory(OutputOptions options) {
    this.options = options;
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
  public Output newOutput(boolean direct, int size) {
    return new ByteBufferOutput(direct, size);
  }

  @Override
  public Output newCompositeOutput() {
    return new CompsiteByteBufferOutput();
  }

  @Override
  public Output wrap(@NonNull ByteBuffer buffer) {
    return new ByteBufferWrappedOutput(buffer);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes) {
    return new ByteBufferWrappedOutput(ByteBuffer.wrap(bytes));
  }

  @Override
  public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return new ByteBufferWrappedOutput(ByteBuffer.wrap(bytes, offset, length));
  }
}
