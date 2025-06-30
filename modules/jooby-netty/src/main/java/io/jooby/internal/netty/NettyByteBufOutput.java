/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.buffer.BufferedOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public interface NettyByteBufOutput extends BufferedOutput {
  @NonNull ByteBuf byteBuf();

  @Override
  @NonNull default ByteBuffer asByteBuffer() {
    return byteBuf().nioBuffer();
  }

  @Override
  @NonNull default String asString(@NonNull Charset charset) {
    return byteBuf().toString(charset);
  }

  @Override
  default void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  default int size() {
    return byteBuf().readableBytes();
  }

  @Override
  default void send(Context ctx) {
    if (ctx instanceof NettyContext netty) {
      netty.send(byteBuf());
    } else {
      ctx.send(asByteBuffer());
    }
  }

  static ByteBuf byteBuf(BufferedOutput output) {
    if (output instanceof NettyByteBufOutput netty) {
      return netty.byteBuf();
    } else {
      return Unpooled.wrappedBuffer(output.asByteBuffer());
    }
  }
}
