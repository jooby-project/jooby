/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

/**
 * To provide Rocker like buffer support
 *
 * @author agentgt
 */
interface JStachioBuffer {

  public ByteBufferedOutputStream acquire();

  public void release(ByteBufferedOutputStream buffer);

  static JStachioBuffer of(int bufferSize, boolean reuseBuffer) {
    if (reuseBuffer) {
      return new ReuseBuffer(bufferSize);
    } else {
      return new NoReuseBuffer(bufferSize);
    }
  }
}

record NoReuseBuffer(int bufferSize) implements JStachioBuffer {
  @Override
  public ByteBufferedOutputStream acquire() {
    return new ByteBufferedOutputStream(bufferSize);
  }

  @Override
  public void release(ByteBufferedOutputStream buffer) {}
}

class ReuseBuffer implements JStachioBuffer {
  private final ThreadLocal<ByteBufferedOutputStream> localBuffer;

  public ReuseBuffer(int bufferSize) {
    super();
    this.localBuffer = ThreadLocal.withInitial(() -> new ByteBufferedOutputStream(bufferSize));
  }

  @Override
  public ByteBufferedOutputStream acquire() {
    return localBuffer.get();
  }

  @Override
  public void release(ByteBufferedOutputStream buffer) {
    buffer.reset();
  }
}
