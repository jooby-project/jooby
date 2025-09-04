/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.output.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public interface NettyByteBufRef extends Output {
  @NonNull ByteBuf byteBuf();

  @Override
  default void transferTo(@NonNull SneakyThrows.Consumer<ByteBuffer> consumer) {
    consumer.accept(asByteBuffer());
  }

  @Override
  @NonNull default ByteBuffer asByteBuffer() {
    return byteBuf().slice().nioBuffer();
  }

  default void send(Context ctx) {
    if (ctx.getClass() == NettyContext.class) {
      var buf = byteBuf();
      ((NettyContext) ctx).send(buf, Integer.toString(buf.readableBytes()));
    } else {
      ctx.send(asByteBuffer());
    }
  }

  static ByteBuf byteBuf(Output output) {
    if (output instanceof NettyByteBufRef netty) {
      return netty.byteBuf();
    } else {
      return Unpooled.wrappedBuffer(output.asByteBuffer());
    }
  }
}
