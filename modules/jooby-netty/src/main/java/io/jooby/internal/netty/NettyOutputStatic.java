/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

public class NettyOutputStatic implements NettyOutputByteBuf {
  private final Supplier<ByteBuf> provider;
  private final AsciiString contentLength;

  protected NettyOutputStatic(int length, Supplier<ByteBuf> provider) {
    this.provider = provider;
    this.contentLength = AsciiString.of(Integer.toString(length));
  }

  @NonNull public ByteBuf byteBuf() {
    return provider.get();
  }

  @Override
  @NonNull public Output write(byte b) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public Output write(byte[] source) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public Output write(byte[] source, int offset, int length) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public Output write(@NonNull String source, @NonNull Charset charset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void send(Context ctx) {
    if (ctx instanceof NettyContext netty) {
      netty.send(provider.get(), contentLength);
    } else {
      ctx.send(asByteBuffer());
    }
  }

  @Override
  public Output write(@NonNull CharBuffer source, @NonNull Charset charset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public Output clear() {
    return this;
  }
}
