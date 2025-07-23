/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;

public class StaticOutput implements Output {

  private final int size;
  private final Supplier<ByteBuffer> provider;

  public StaticOutput(int size, Supplier<ByteBuffer> provider) {
    this.size = size;
    this.provider = provider;
  }

  public StaticOutput(ByteBuffer byteBuffer) {
    this(byteBuffer.remaining(), () -> byteBuffer);
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
    var buffer = provider.get();
    return buffer.slice().asReadOnlyBuffer();
  }

  @Override
  public String toString() {
    return "size=" + size();
  }

  @Override
  public void send(Context ctx) {
    ctx.send(provider.get());
  }
}
