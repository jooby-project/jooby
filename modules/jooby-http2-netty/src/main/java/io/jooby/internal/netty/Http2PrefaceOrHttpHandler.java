/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

class Http2PrefaceOrHttpHandler extends ByteToMessageDecoder {

  private static final int PRI = 0x50524920;

  private Consumer<ChannelPipeline> http1;

  private Consumer<ChannelPipeline> http2;

  public Http2PrefaceOrHttpHandler(Consumer<ChannelPipeline> http1,
      Consumer<ChannelPipeline> http2) {
    this.http1 = http1;
    this.http2 = http2;
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    if (in.readableBytes() < 4) {
      return;
    }

    if (in.getInt(in.readerIndex()) == PRI) {
      http2.accept(ctx.pipeline());
    } else {
      http1.accept(ctx.pipeline());
    }

    ctx.pipeline().remove(this);
  }
}
