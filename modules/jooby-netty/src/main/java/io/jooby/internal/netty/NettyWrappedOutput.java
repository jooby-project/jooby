/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;

public class NettyWrappedOutput implements NettyByteBufOutput {

  private final ByteBuf buffer;

  protected NettyWrappedOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @NonNull public ByteBuf byteBuf() {
    return this.buffer;
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
  public Output write(@NonNull CharBuffer source, @NonNull Charset charset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull public Output clear() {
    return this;
  }
}
