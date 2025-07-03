/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.buffer.BufferedOutput;

public class ByteArrayWrappedOutput implements BufferedOutput {

  private final byte[] buffer;

  public ByteArrayWrappedOutput(byte[] source) {
    this.buffer = source;
  }

  @Override
  public BufferedOutput write(byte b) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BufferedOutput write(byte[] source) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BufferedOutput write(byte[] source, int offset, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BufferedOutput clear() {
    return this;
  }

  @Override
  public int size() {
    return buffer.length;
  }

  @Override
  public void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(ByteBuffer.wrap(buffer));
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return ByteBuffer.wrap(buffer);
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
