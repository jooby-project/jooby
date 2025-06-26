/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;

public class ByteBufferOutputFactory implements OutputFactory {
  @Override
  public Output newBufferedOutput(int size) {
    return new ByteBufferOutputImpl(size);
  }

  @Override
  public ChunkedOutput newChunkedOutput() {
    return new ByteBufferChunkedOutput();
  }

  @Override
  public Output wrap(ByteBuffer buffer) {
    return new WrappedOutput(buffer);
  }

  @Override
  public Output wrap(byte[] bytes) {
    return new WrappedOutput(bytes);
  }

  @Override
  public Output wrap(byte[] bytes, int offset, int length) {
    return new WrappedOutput(bytes, offset, length);
  }
}
