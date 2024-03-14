/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.ServerOptions._4KB;
import static io.jooby.ServerOptions._8KB;

import java.util.function.Supplier;

import io.jooby.internal.netty.http2.NettyHttp2Configurer;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {
  private static final String H2_HANDSHAKE = "h2-handshake";
  private Integer compressionLevel;
  private int bufferSize;
  private long maxRequestSize;
  private SslContext sslContext;
  private boolean is100ContinueExpected;
  private boolean http2;
  private Supplier<NettyHandler> handlerFactory;

  public NettyPipeline(
      Supplier<NettyHandler> handlerFactory,
      SslContext sslContext,
      Integer compressionLevel,
      int bufferSize,
      long maxRequestSize,
      boolean http2,
      boolean is100ContinueExpected) {
    this.sslContext = sslContext;
    this.compressionLevel = compressionLevel;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
    this.is100ContinueExpected = is100ContinueExpected;
    this.http2 = http2;
    this.handlerFactory = handlerFactory;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    p.addLast(new FlushConsolidationHandler());
    if (sslContext != null) {
      p.addLast("ssl", sslContext.newHandler(ch.alloc()));
    }
    if (http2) {
      Http2Settings settings = new Http2Settings(maxRequestSize, sslContext != null);
      Http2Extension extension =
          new Http2Extension(
              settings, this::http11, this::http11Upgrade, this::http2, this::http2c);
      NettyHttp2Configurer configurer = new NettyHttp2Configurer();
      ChannelInboundHandler handshake = configurer.configure(extension);

      p.addLast(H2_HANDSHAKE, handshake);

      setupExpectContinue(p);

      setupCompression(p);

      p.addLast("handler", handlerFactory.get());
    } else {
      http11(p);
    }
  }

  private void setupExpectContinue(ChannelPipeline p) {
    if (is100ContinueExpected) {
      p.addLast("expect-continue", new HttpServerExpectContinueHandler());
    }
  }

  private void setupCompression(ChannelPipeline p) {
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
    HttpServerCodec serverCodec = createServerCodec();
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
    HttpServerCodec codec = createServerCodec();
    p.addLast("codec", codec);
    setupExpectContinue(p);
    setupCompression(p);
    p.addLast("handler", handlerFactory.get());
  }

  HttpServerCodec createServerCodec() {
    return new HttpServerCodec(_4KB, _8KB, bufferSize);
  }
}
