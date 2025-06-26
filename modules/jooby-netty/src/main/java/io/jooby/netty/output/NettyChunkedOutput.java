/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty.output;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.internal.netty.NettyContext;
import io.jooby.output.ChunkedOutput;
import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;

class NettyChunkedOutput implements ChunkedOutput, NettyOutput {

  private final CompositeByteBuf buffer;

  protected NettyChunkedOutput(CompositeByteBuf buffer) {
    this.buffer = buffer;
  }

  @Override
  public ByteBuf byteBuf() {
    return this.buffer;
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return this.buffer.nioBuffer();
  }

  @Override
  public String asString(@NonNull Charset charset) {
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
  public Output write(byte b) {
    buffer.writeByte(b);
    return this;
  }

  @Override
  public Output write(byte[] source) {
    buffer.writeBytes(source);
    return this;
  }

  @Override
  public Output write(byte[] source, int offset, int length) {
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
  public void close() throws IOException {}

  @Override
  public List<ByteBuffer> getChunks() {
    return Arrays.asList(buffer.nioBuffers());
  }
}
