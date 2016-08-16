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

import java.util.concurrent.TimeUnit;

import org.jooby.spi.HttpHandler;

import com.typesafe.config.Config;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {

  // private static class H2 implements UpgradeCodecFactory {
  //
  // private NettyInitializer initializer;
  //
  // public H2(final NettyInitializer initializer) {
  // this.initializer = initializer;
  // }
  //
  // @Override
  // public UpgradeCodec newUpgradeCodec(final CharSequence protocol) {
  // if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
  // return new Http2ServerUpgradeCodec(new Http2Codec(true, new HelloWorldHttp2Handler()));
  // } else {
  // return null;
  // }
  // }
  //
  // }

  private EventExecutorGroup executor;

  private HttpHandler handler;

  private Config config;

  private int maxInitialLineLength;

  private int maxHeaderSize;

  private int maxChunkSize;

  int maxContentLength;

  private long idleTimeOut;

  private SslContext sslCtx;

  private boolean http2;

  public NettyInitializer(final EventExecutorGroup executor, final HttpHandler handler,
      final Config config, final SslContext sslCtx, final boolean http2) {
    this.executor = executor;
    this.handler = handler;
    this.config = config;

    maxInitialLineLength = config.getBytes("netty.http.MaxInitialLineLength").intValue();
    maxHeaderSize = config.getBytes("netty.http.MaxHeaderSize").intValue();
    maxChunkSize = config.getBytes("netty.http.MaxChunkSize").intValue();
    maxContentLength = config.getBytes("netty.http.MaxContentLength").intValue();
    idleTimeOut = config.getDuration("netty.http.IdleTimeout", TimeUnit.MILLISECONDS);
    this.sslCtx = sslCtx;
    this.http2 = http2;
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    if (sslCtx != null) {
      if (http2) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new NettyHttp2Handler(this));
      } else {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
      }
    } else {
      // if (http2) {
      // HttpServerCodec sourceCodec = new HttpServerCodec();
      // ch.pipeline().addLast(sourceCodec);
      // ch.pipeline()
      // .addLast(new HttpServerUpgradeHandler(sourceCodec, new H2(this), Integer.MAX_VALUE));
      // } else {
      pipeline(ch.pipeline(), true);
      // }
    }
  }

  public void pipeline(final ChannelPipeline pipeline, final boolean http1) {

    if (http1) {
      pipeline
          .addLast("decoder",
              new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, false))
          .addLast("encoder", new HttpResponseEncoder());

      if (idleTimeOut > 0) {
        pipeline.addLast("timeout", new IdleStateHandler(0, 0, idleTimeOut, TimeUnit.MILLISECONDS));
      }

      pipeline
          .addLast("aggregator", new HttpObjectAggregator(maxContentLength));
    }

    pipeline.addLast(executor, "handler", new NettyHandler(handler, config));
  }

}
