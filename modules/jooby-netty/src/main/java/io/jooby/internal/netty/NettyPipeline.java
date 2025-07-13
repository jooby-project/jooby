/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.List;
import java.util.function.Supplier;

import io.jooby.Jooby;
import io.jooby.internal.netty.http2.NettyHttp2Configurer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {
  private static final String H2_HANDSHAKE = "h2-handshake";
  private final SslContext sslContext;
  private final NettyDateService serverDate;
  private final HttpDecoderConfig decoderConfig;
  private final List<Jooby> applications;
  private final long maxRequestSize;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final boolean http2;
  private final boolean expectContinue;
  private final Integer compressionLevel;

  public NettyPipeline(
      SslContext sslContext,
      NettyDateService dateService,
      HttpDecoderConfig decoderConfig,
      List<Jooby> applications,
      long maxRequestSize,
      int bufferSize,
      boolean defaultHeaders,
      boolean http2,
      boolean expectContinue,
      Integer compressionLevel) {
    this.sslContext = sslContext;
    this.serverDate = dateService;
    this.decoderConfig = decoderConfig;
    this.applications = applications;
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
    this.expectContinue = expectContinue;
    this.compressionLevel = compressionLevel;
  }

  private NettyHandler createHandler() {
    return new NettyHandler(
        serverDate, applications, maxRequestSize, bufferSize, defaultHeaders, http2);
  }

  @Override
  public void initChannel(SocketChannel ch) {
    var p = ch.pipeline();
    if (sslContext != null) {
      p.addLast("ssl", sslContext.newHandler(ch.alloc()));
    }
    // https://github.com/jooby-project/jooby/issues/3433:
    // using new FlushConsolidationHandler(DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true)
    // cause the bug, for now I'm going to remove flush consolidating handler... doesn't seem to
    // help much
    // p.addLast(new FlushConsolidationHandler(DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, false));
    if (http2) {
      var settings = new Http2Settings(maxRequestSize, sslContext != null);
      var extension =
          new Http2Extension(
              settings, this::http11, this::http11Upgrade, this::http2, this::http2c);
      var configurer = new NettyHttp2Configurer();
      var handshake = configurer.configure(extension);

      p.addLast(H2_HANDSHAKE, handshake);
      additionalHandlers(p);
      p.addLast("handler", createHandler());
    } else {
      http11(p);
    }
  }

  private void additionalHandlers(ChannelPipeline p) {
    if (expectContinue) {
      p.addLast("expect-continue", new HttpServerExpectContinueHandler());
    }
    if (compressionLevel != null) {
      p.addLast("compressor", new HttpChunkContentCompressor(compressionLevel));
      p.addLast("ws-compressor", new NettyWebSocketCompressor(compressionLevel));
    }
  }

  private void http2(ChannelPipeline pipeline, Supplier<ChannelOutboundHandler> factory) {
    pipeline.addAfter(H2_HANDSHAKE, "http2", factory.get());
  }

  private void http2c(ChannelPipeline pipeline, Supplier<ChannelOutboundHandler> factory) {
    pipeline.addAfter(H2_HANDSHAKE, "http2", factory.get());
  }

  private void http11Upgrade(
      ChannelPipeline pipeline, Supplier<HttpServerUpgradeHandler.UpgradeCodec> factory) {
    // direct http1 to h2c
    HttpServerCodec serverCodec = new HttpServerCodec(decoderConfig);
    pipeline.addAfter(H2_HANDSHAKE, "codec", serverCodec);
    pipeline.addAfter(
        "codec",
        "h2upgrade",
        new HttpServerUpgradeHandler(
            serverCodec,
            protocol -> protocol.toString().equals("h2c") ? factory.get() : null,
            (int) maxRequestSize));
  }

  private void http11(ChannelPipeline p) {
    p.addLast("decoder", new NettyRequestDecoder(decoderConfig));
    p.addLast("encoder", new NettyResponseEncoder());
    additionalHandlers(p);
    p.addLast("handler", createHandler());
  }
}
