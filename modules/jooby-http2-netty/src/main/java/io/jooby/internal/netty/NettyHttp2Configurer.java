/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.netty.handler.codec.http.HttpScheme.HTTP;

import io.jooby.Http2Configurer;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;

public class NettyHttp2Configurer
    implements Http2Configurer<Http2Extension, ChannelInboundHandler> {

  @Override public boolean support(Class type) {
    return type == Http2Extension.class;
  }

  @Override public ChannelInboundHandler configure(Http2Extension extension) {
    if (extension.isSecure()) {
      return new Http2OrHttp11Handler(extension::http11,
          pipeline ->
              extension.http2(pipeline,
                  settings -> newHttp2Handler(settings.getMaxRequestSize(), HttpScheme.HTTPS))
      );
    } else {
      return new Http2PrefaceOrHttpHandler(
          pipeline ->
              extension.http11Upgrade(pipeline, settings ->
                  new Http2ServerUpgradeCodec(newHttp2Handler(settings.getMaxRequestSize(), HTTP))),

          pipeline ->
              extension.http2c(pipeline, settings ->
                  newHttp2Handler(settings.getMaxRequestSize(), HTTP))
      );
    }
  }

  private Http2ConnectionHandler newHttp2Handler(int maxRequestSize, HttpScheme scheme) {
    DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
    InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection)
        .propagateSettings(false)
        .validateHttpHeaders(true)
        .maxContentLength(maxRequestSize)
        .build();

    return new HttpToHttp2ConnectionHandlerBuilder()
        .frameListener(listener)
        .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
        .connection(connection)
        .httpScheme(scheme)
        .build();
  }
}
