/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.output;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;

public class OutputByteArrayStatic implements Output {
  private final byte[] bytes;
  private final int offset;
  private final int length;

  public OutputByteArrayStatic(byte[] bytes, int offset, int length) {
    this.bytes = bytes;
    this.offset = offset;
    this.length = length;
  }

  public OutputByteArrayStatic(byte[] bytes) {
    this(bytes, 0, bytes.length);
  }

  @Override
  public int size() {
    return length - offset;
  }

  @Override
  public void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return ByteBuffer.wrap(bytes, offset, length);
  }

  @Override
  public String toString() {
    return "size=" + size();
  }

  @Override
  public void send(Context ctx) {
    ctx.send(ByteBuffer.wrap(bytes, offset, length));
  }
}
