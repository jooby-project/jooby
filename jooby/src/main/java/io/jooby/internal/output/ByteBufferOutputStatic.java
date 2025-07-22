/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;

public class ByteBufferOutputStatic implements Output {

  private final int size;
  private final Supplier<ByteBuffer> provider;

  public ByteBufferOutputStatic(int size, Supplier<ByteBuffer> provider) {
    this.size = size;
    this.provider = provider;
  }

  public ByteBufferOutputStatic(ByteBuffer byteBuffer) {
    this(byteBuffer.remaining(), () -> byteBuffer);
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
    return this;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return provider.get();
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
    ctx.send(asByteBuffer());
  }
}
