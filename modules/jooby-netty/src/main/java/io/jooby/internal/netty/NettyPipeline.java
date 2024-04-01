/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.netty.handler.flush.FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import io.jooby.Router;
import io.jooby.internal.netty.http2.NettyHttp2Configurer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {
  private static final String H2_HANDSHAKE = "h2-handshake";
  private final SslContext sslContext;
  private final ScheduledExecutorService scheduler;
  private final HttpDataFactory httpDataFactory;
  private final HttpDecoderConfig decoderConfig;
  private final Router router;
  private final long maxRequestSize;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final boolean http2;
  private final boolean expectContinue;
  private final Integer compressionLevel;

  public NettyPipeline(
      SslContext sslContext,
      ScheduledExecutorService scheduler,
      HttpDataFactory httpDataFactory,
      HttpDecoderConfig decoderConfig,
      Router router,
      long maxRequestSize,
      int bufferSize,
      boolean defaultHeaders,
      boolean http2,
      boolean expectContinue,
      Integer compressionLevel) {
    this.sslContext = sslContext;
    this.scheduler = scheduler;
    this.httpDataFactory = httpDataFactory;
    this.decoderConfig = decoderConfig;
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
    this.expectContinue = expectContinue;
    this.compressionLevel = compressionLevel;
  }

  private NettyHandler createHandler() {
    return new NettyHandler(
        scheduler, router, maxRequestSize, bufferSize, httpDataFactory, defaultHeaders, http2);
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    if (sslContext != null) {
      p.addLast("ssl", sslContext.newHandler(ch.alloc()));
    }
    p.addLast(new FlushConsolidationHandler(DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true));
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
    p.addLast("decoder", new HttpRequestDecoder(decoderConfig));
    p.addLast("encoder", new NettyResponseEncoder());
    additionalHandlers(p);
    p.addLast("handler", createHandler());
  }
}
