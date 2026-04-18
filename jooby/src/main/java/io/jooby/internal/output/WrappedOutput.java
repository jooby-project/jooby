/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

/** This is part of {@link OutputFactory#getContextFactory()}. */
public record WrappedOutput(ByteBuffer buffer) implements Output {

  @Override
  public int size() {
    return buffer.remaining();
  }

  @Override
  public void transferTo(SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return buffer.slice();
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
