/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;

class ByteBufferWrappedOutput implements Output {

  private final ByteBuffer buffer;

  public ByteBufferWrappedOutput(byte[] source, int offset, int length) {
    this(ByteBuffer.wrap(source, offset, length));
  }

  public ByteBufferWrappedOutput(byte[] source) {
    this(source, 0, source.length);
  }

  public ByteBufferWrappedOutput(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public Output write(byte b) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Output write(byte[] source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Output write(byte[] source, int offset, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Output clear() {
    buffer.clear();
    return this;
  }

  @Override
  public int size() {
    return buffer.remaining();
  }

  @Override
  public void accept(SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(buffer);
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return buffer.duplicate();
  }

  @Override
  public String asString(@NonNull Charset charset) {
    return charset.decode(asByteBuffer()).toString();
  }

  @Override
  public String toString() {
    return "size=" + size();
  }

  @Override
  public void send(Context ctx) {
    ctx.send(buffer);
  }
}
