/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.buffer.BufferedOutput;
import io.netty.buffer.ByteBuf;

public class NettyBufferedOutput implements NettyByteBufOutput {

  private final ByteBuf buffer;

  protected NettyBufferedOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @NonNull public ByteBuf byteBuf() {
    return this.buffer;
  }

  @Override
  @NonNull public BufferedOutput write(byte b) {
    buffer.writeByte(b);
    return this;
  }

  @Override
  @NonNull public BufferedOutput write(byte[] source) {
    buffer.writeBytes(source);
    return this;
  }

  @Override
  @NonNull public BufferedOutput write(byte[] source, int offset, int length) {
    this.buffer.writeBytes(source, offset, length);
    return this;
  }

  @Override
  @NonNull public BufferedOutput write(@NonNull String source, @NonNull Charset charset) {
    this.buffer.writeBytes(source.getBytes(charset));
    return this;
  }

  @Override
  public BufferedOutput write(@NonNull CharBuffer source, @NonNull Charset charset) {
    this.buffer.writeBytes(charset.encode(source));
    return this;
  }

  @Override
  @NonNull public BufferedOutput clear() {
    this.buffer.clear();
    return this;
  }
}
