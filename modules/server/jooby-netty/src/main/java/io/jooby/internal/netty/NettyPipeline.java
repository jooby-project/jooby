/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;

import java.util.function.Supplier;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {

  private final SslContext sslCtx;
  private final Supplier<ChannelInboundHandler> handler;
  private final boolean gzip;
  private final long maxRequestSize;

  public NettyPipeline(SslContext sslCtx, Supplier<ChannelInboundHandler> handler, boolean gzip,
      long maxRequestSize) {
    this.sslCtx = sslCtx;
    this.handler = handler;
    this.gzip = gzip;
    this.maxRequestSize = maxRequestSize;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    if (sslCtx != null) {
      p.addLast(sslCtx.newHandler(ch.alloc()));
    }
    p.addLast("flusher", new FlushConsolidationHandler(256, true));
    p.addLast("codec", new HttpServerCodec());
    if (gzip) {
      p.addLast("gzip", new HttpContentCompressor());
    }
    p.addLast("handler", handler.get());
  }
}
