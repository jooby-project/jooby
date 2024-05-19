/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.nio.charset.StandardCharsets;

import io.jstach.jstachio.output.ByteBufferEncodedOutput;

/**
 * To provide Rocker like buffer support
 *
 * @author agentgt
 */
interface JStachioBuffer {

  ByteBufferEncodedOutput acquire();

  void release(ByteBufferEncodedOutput buffer);

  static JStachioBuffer of(int bufferSize) {
    return new NoReuseBuffer(bufferSize);
  }
}

record NoReuseBuffer(int bufferSize) implements JStachioBuffer {
  @Override
  public ByteBufferEncodedOutput acquire() {
    return ByteBufferEncodedOutput.ofByteArray(StandardCharsets.UTF_8, bufferSize);
  }

  @Override
  public void release(ByteBufferEncodedOutput buffer) {}
}
