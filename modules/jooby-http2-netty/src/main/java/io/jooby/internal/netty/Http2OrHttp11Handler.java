/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

class Http2OrHttp11Handler extends ApplicationProtocolNegotiationHandler {

  private final Consumer<ChannelPipeline> http2;
  private final Consumer<ChannelPipeline> http1;

  public Http2OrHttp11Handler(Consumer<ChannelPipeline> http1, Consumer<ChannelPipeline> http2) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.http2 = http2;
    this.http1 = http1;
  }

  @Override
  public void configurePipeline(final ChannelHandlerContext ctx, final String protocol) {
    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      http1.accept(ctx.pipeline());
    } else if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      http2.accept(ctx.pipeline());
    } else {
      throw new IllegalStateException("Unknown protocol: " + protocol);
    }
  }

}
