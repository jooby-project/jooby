/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jooby.spi.HttpHandler;

import com.typesafe.config.Config;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {

  private EventExecutorGroup executor;

  private HttpHandler handler;

  private Config config;

  private int maxInitialLineLength;

  private int maxHeaderSize;

  private int maxChunkSize;

  int maxContentLength;

  private long idleTimeOut;

  private SslContext sslCtx;

  private boolean supportH2;

  public NettyPipeline(final EventExecutorGroup executor, final HttpHandler handler,
      final Config conf, final SslContext sslCtx) {
    this.executor = executor;
    this.handler = handler;
    this.config = conf;

    maxInitialLineLength = conf.getBytes("netty.http.MaxInitialLineLength").intValue();
    maxHeaderSize = conf.getBytes("netty.http.MaxHeaderSize").intValue();
    maxChunkSize = conf.getBytes("netty.http.MaxChunkSize").intValue();
    maxContentLength = conf.getBytes("netty.http.MaxContentLength").intValue();
    idleTimeOut = conf.getDuration("netty.http.IdleTimeout", TimeUnit.MILLISECONDS);
    supportH2 = conf.getBoolean("server.http2.enabled");
    this.sslCtx = sslCtx;
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    final ChannelPipeline p = ch.pipeline();
    if (sslCtx != null) {
      p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
      p.addLast("h1.1/h2", new Http2OrHttpHandler());
    } else {
      if (supportH2) {
        p.addLast("h2c", new Http2PrefaceOrHttpHandler());

        idle(p);

        aggregator(p);

        jooby(p);
      } else {
        http1(p);
      }
    }
  }

  private void idle(final ChannelPipeline p) {
    if (idleTimeOut > 0) {
      p.addLast("timeout", new IdleStateHandler(0, 0, idleTimeOut, TimeUnit.MILLISECONDS));
    }
  }

  private HttpServerCodec http1Codec() {
    return new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, false);
  }

  private void http2(final ChannelPipeline p) {
    p.addLast("h2", newHttp2ConnectionHandler(p));

    idle(p);

    jooby(p);
  }

  private void http1(final ChannelPipeline p) {
    p.addLast("codec", http1Codec());

    idle(p);

    aggregator(p);

    jooby(p);
  }

  private void aggregator(final ChannelPipeline p) {
    p.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
  }

  private void jooby(final ChannelPipeline p) {
    p.addLast(executor, "jooby", new NettyHandler(handler, config));
  }

  private Http2ConnectionHandler newHttp2ConnectionHandler(final ChannelPipeline p) {
    DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
    InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection)
        .propagateSettings(false)
        .validateHttpHeaders(false)
        .maxContentLength(maxContentLength)
        .build();

    HttpToHttp2ConnectionHandler http2handler = new HttpToHttp2ConnectionHandlerBuilder()
        .frameListener(listener)
        .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
        .connection(connection)
        .build();

    return http2handler;
  }

  class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

    public Http2OrHttpHandler() {
      super(ApplicationProtocolNames.HTTP_1_1);
    }

    @Override
    public void configurePipeline(final ChannelHandlerContext ctx, final String protocol)
        throws Exception {
      if (supportH2 && ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        http2(ctx.pipeline());
      } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        http1(ctx.pipeline());
      } else {
        throw new IllegalStateException("Unknown protocol: " + protocol);
      }
    }

  }

  class Http2PrefaceOrHttpHandler extends ByteToMessageDecoder {

    private static final int PRI = 0x50524920;

    private String name;

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
      super.handlerAdded(ctx);
      name = ctx.name();
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws Exception {
      if (in.readableBytes() < 4) {
        return;
      }

      if (in.getInt(in.readerIndex()) == PRI) {
        h2c(ctx);
      } else {
        h2cOrHttp1(ctx);
      }

      ctx.pipeline().remove(this);
    }

    private void h2cOrHttp1(final ChannelHandlerContext ctx) {
      ChannelPipeline p = ctx.pipeline();
      HttpServerCodec http1codec = http1Codec();

      String baseName = name;
      baseName = addAfter(p, baseName, "codec", http1codec);
      baseName = addAfter(p, baseName, "h2upgrade",
          new HttpServerUpgradeHandler(http1codec, protocol -> {
            if (!AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
              return null;
            }
            return new Http2ServerUpgradeCodec(newHttp2ConnectionHandler(p));
          }, maxContentLength));
    }

    private void h2c(final ChannelHandlerContext ctx) {
      final ChannelPipeline p = ctx.pipeline();
      addAfter(p, name, "h2", newHttp2ConnectionHandler(p));
    }

    private String addAfter(final ChannelPipeline p, final String baseName, final String name,
        final ChannelHandler h) {
      p.addAfter(baseName, name, h);
      return p.context(h).name();
    }
  }
}
