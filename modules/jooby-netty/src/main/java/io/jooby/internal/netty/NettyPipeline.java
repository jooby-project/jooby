/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import io.jooby.Context;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {
  private static final String H2_HANDSHAKE = "h2-handshake";

  private final SslContext sslContext;
  private final HttpDecoderConfig decoderConfig;
  private final Context.Selector contextSelector;
  private final long maxRequestSize;
  private final int maxFormFields;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final boolean http2;
  private final boolean expectContinue;
  private final Integer compressionLevel;

  public NettyPipeline(
      SslContext sslContext,
      HttpDecoderConfig decoderConfig,
      Context.Selector contextSelector,
      long maxRequestSize,
      int maxFormFields,
      int bufferSize,
      boolean defaultHeaders,
      boolean http2,
      boolean expectContinue,
      Integer compressionLevel) {
    this.sslContext = sslContext;
    this.decoderConfig = decoderConfig;
    this.contextSelector = contextSelector;
    this.maxRequestSize = maxRequestSize;
    this.maxFormFields = maxFormFields;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
    this.expectContinue = expectContinue;
    this.compressionLevel = compressionLevel;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();

    if (sslContext != null) {
      p.addLast("ssl", sslContext.newHandler(ch.alloc()));
    }

    if (http2) {
      p.addLast(H2_HANDSHAKE, setupHttp2Handshake(sslContext != null));
    } else {
      setupHttp11(p);
    }
  }

  private void setupHttp11(ChannelPipeline p) {
    p.addLast("codec", createServerCodec());
    addCommonHandlers(p);
    p.addLast("handler", createHandler(p.channel().eventLoop()));
  }

  private void setupHttp2(ChannelPipeline pipeline) {
    var frameCodec =
        Http2FrameCodecBuilder.forServer()
            .initialSettings(Http2Settings.defaultSettings().maxFrameSize((int) maxRequestSize))
            .build();

    pipeline.addLast("http2-codec", frameCodec);
    pipeline.addLast(
        "http2-multiplex", new Http2MultiplexHandler(new Http2StreamInitializer(this)));
  }

  private void setupHttp11Upgrade(ChannelPipeline pipeline) {
    var serverCodec = createServerCodec();
    pipeline.addLast("codec", serverCodec);

    pipeline.addLast(
        "h2upgrade",
        new HttpServerUpgradeHandler(
            serverCodec,
            protocol -> "h2c".equals(protocol.toString()) ? createH2CUpgradeCodec() : null,
            (int) maxRequestSize));

    addCommonHandlers(pipeline);
    pipeline.addLast("handler", createHandler(pipeline.channel().eventLoop()));
  }

  private ChannelInboundHandler setupHttp2Handshake(boolean secure) {
    if (secure) {
      return new AlpnHandler(this);
    }
    return new Http2PrefaceOrHttpHandler(this);
  }

  private void addCommonHandlers(ChannelPipeline p) {
    if (expectContinue) {
      p.addLast("expect-continue", new HttpServerExpectContinueHandler());
    }
    if (compressionLevel != null) {
      p.addLast("compressor", new HttpChunkContentCompressor(compressionLevel));
      p.addLast("ws-compressor", new NettyWebSocketCompressor(compressionLevel));
    }
  }

  private Http2ServerUpgradeCodec createH2CUpgradeCodec() {
    return new Http2ServerUpgradeCodec(
        Http2FrameCodecBuilder.forServer().build(),
        new Http2MultiplexHandler(new Http2StreamInitializer(this)));
  }

  private NettyHandler createHandler(ScheduledExecutorService executor) {
    return new NettyHandler(
        new NettyDateService(executor),
        contextSelector,
        maxRequestSize,
        maxFormFields,
        bufferSize,
        defaultHeaders,
        http2);
  }

  private NettyServerCodec createServerCodec() {
    return new NettyServerCodec(decoderConfig);
  }

  /** Handles the transition from ALPN to H1 or H2 */
  private static class AlpnHandler extends ApplicationProtocolNegotiationHandler {
    private final NettyPipeline pipeline;

    AlpnHandler(NettyPipeline pipeline) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.pipeline = pipeline;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
      if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        pipeline.setupHttp2(ctx.pipeline());
      } else {
        pipeline.setupHttp11(ctx.pipeline());
      }
    }
  }

  /** Detects HTTP/2 connection preface or upgrades to H1/H2C */
  private static class Http2PrefaceOrHttpHandler extends ByteToMessageDecoder {
    private static final int PRI = 0x50524920; // "PRI "
    private final NettyPipeline pipeline;

    Http2PrefaceOrHttpHandler(NettyPipeline pipeline) {
      this.pipeline = pipeline;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
      if (in.readableBytes() < 4) return;

      if (in.getInt(in.readerIndex()) == PRI) {
        pipeline.setupHttp2(ctx.pipeline());
      } else {
        pipeline.setupHttp11Upgrade(ctx.pipeline());
      }
      ctx.pipeline().remove(this);
    }
  }

  /** Initializes the child channels created for each HTTP/2 stream */
  private static class Http2StreamInitializer extends ChannelInitializer<Channel> {
    private final NettyPipeline pipeline;

    Http2StreamInitializer(NettyPipeline pipeline) {
      this.pipeline = pipeline;
    }

    @Override
    protected void initChannel(Channel ch) {
      ch.pipeline().addLast("http2", new Http2StreamFrameToHttpObjectCodec(true));
      ch.pipeline().addLast("handler", pipeline.createHandler(ch.eventLoop()));
    }
  }
}
