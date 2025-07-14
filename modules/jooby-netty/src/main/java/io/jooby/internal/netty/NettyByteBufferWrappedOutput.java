/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.buffer.BufferedOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;

public class NettyByteBufferWrappedOutput implements NettyByteBufOutput {

  private final ByteBuffer buffer;
  private final AsciiString contentLength;

  protected NettyByteBufferWrappedOutput(ByteBuffer buffer) {
    this.buffer = buffer;
    this.contentLength = AsciiString.of(Integer.toString(buffer.remaining()));
  }

  @NonNull public ByteBuf byteBuf() {
    return Unpooled.wrappedBuffer(buffer);
  }

  @Override
  @NonNull public BufferedOutput write(byte b) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public BufferedOutput write(byte[] source) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public BufferedOutput write(byte[] source, int offset, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public BufferedOutput write(@NonNull String source, @NonNull Charset charset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void send(Context ctx) {
    if (ctx instanceof NettyContext netty) {
      netty.send(Unpooled.wrappedBuffer(buffer), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }

  @Override
  public BufferedOutput write(@NonNull CharBuffer source, @NonNull Charset charset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public BufferedOutput clear() {
    return this;
  }
}
