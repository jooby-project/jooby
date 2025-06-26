/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.output;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.internal.netty.NettyContext;
import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;

class NettyBufferedOutput implements NettyOutput {

  private final ByteBuf buffer;

  protected NettyBufferedOutput(ByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  @NonNull public ByteBuf byteBuf() {
    return this.buffer;
  }

  @Override
  @NonNull public ByteBuffer asByteBuffer() {
    return this.buffer.nioBuffer();
  }

  @Override
  @NonNull public String asString(@NonNull Charset charset) {
    return this.buffer.toString(charset);
  }

  @Override
  public void accept(SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  public int size() {
    return buffer.readableBytes();
  }

  @Override
  @NonNull public Output write(byte b) {
    buffer.writeByte(b);
    return this;
  }

  @Override
  @NonNull public Output write(byte[] source) {
    buffer.writeBytes(source);
    return this;
  }

  @Override
  @NonNull public Output write(byte[] source, int offset, int length) {
    this.buffer.writeBytes(source, offset, length);
    return this;
  }

  @Override
  public void send(Context ctx) {
    if (ctx instanceof NettyContext netty) {
      netty.send(this.buffer);
    } else {
      ctx.send(this.buffer.nioBuffer());
    }
  }

  @Override
  @NonNull public Output clear() {
    this.buffer.clear();
    return this;
  }
}
