/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;

public class DefaultOutputFactory implements OutputFactory {
  @Override
  public Output newBufferedOutput(int size) {
    return new ByteBufferOut(size);
  }

  @Override
  public Output newCompositeOutput() {
    return new CompsiteByteBufferOutput();
  }

  @Override
  public Output wrap(ByteBuffer buffer) {
    return new ByteBufferWrappedOutput(buffer);
  }

  @Override
  public Output wrap(byte[] bytes) {
    return new ByteBufferWrappedOutput(bytes);
  }

  @Override
  public Output wrap(byte[] bytes, int offset, int length) {
    return new ByteBufferWrappedOutput(bytes, offset, length);
  }
}
