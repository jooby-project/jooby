/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import io.jooby.output.BufferedOutput;
import io.netty.buffer.ByteBuf;

public record NettyByteBufOutput(ByteBuf buffer) implements BufferedOutput, NettyByteBufRef {

  @Override
  public BufferedOutput write(byte b) {
    buffer.writeByte(b);
    return this;
  }

  @Override
  public BufferedOutput write(byte[] source) {
    buffer.writeBytes(source);
    return this;
  }

  @Override
  public BufferedOutput write(byte[] source, int offset, int length) {
    this.buffer.writeBytes(source, offset, length);
    return this;
  }

  @Override
  public BufferedOutput write(String source, Charset charset) {
    this.buffer.writeBytes(source.getBytes(charset));
    return this;
  }

  @Override
  public BufferedOutput write(CharBuffer source, Charset charset) {
    this.buffer.writeBytes(charset.encode(source));
    return this;
  }

  @Override
  public BufferedOutput clear() {
    this.buffer.clear();
    return this;
  }

  @Override
  public int size() {
    return buffer.readableBytes();
  }

  @Override
  public ByteBuf byteBuf() {
    return buffer;
  }
}
