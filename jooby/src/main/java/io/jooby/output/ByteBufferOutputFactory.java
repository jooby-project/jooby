/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.output.ByteArrayWrappedOutput;
import io.jooby.internal.output.ByteBufferOutput;
import io.jooby.internal.output.ByteBufferWrappedOutput;
import io.jooby.internal.output.CompsiteByteBufferOutput;

/**
 * An output factory backed by {@link ByteBuffer}.
 *
 * @author edgar
 * @since 4.0.0
 */
public class ByteBufferOutputFactory implements OutputFactory {
  private int initialBufferSize;
  private final boolean direct;

  public ByteBufferOutputFactory(boolean direct, int initialBufferSize) {
    this.initialBufferSize = initialBufferSize;
    this.direct = direct;
  }

  @Override
  public int getInitialBufferSize() {
    return initialBufferSize;
  }

  @Override
  public OutputFactory setInitialBufferSize(int initialBufferSize) {
    this.initialBufferSize = initialBufferSize;
    return this;
  }

  @Override
  public boolean isDirect() {
    return direct;
  }

  @Override
  public Output newBufferedOutput(boolean direct, int size) {
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
    return new ByteArrayWrappedOutput(bytes);
  }

  @Override
  public Output wrap(@NonNull byte[] bytes, int offset, int length) {
    return new ByteBufferWrappedOutput(ByteBuffer.wrap(bytes, offset, length));
  }
}
