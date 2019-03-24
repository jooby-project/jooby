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

import io.jooby.Router;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.function.Consumer;

import static io.jooby.Server._4KB;
import static io.jooby.Server._8KB;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {

  private final Router router;
  private final HttpDataFactory factory;
  private final boolean gzip;
  private final int bufferSize;
  private final long maxRequestSize;
  private final Consumer<HttpHeaders> defaultHeaders;

  public NettyPipeline(Router router, HttpDataFactory factory, Consumer<HttpHeaders> defaultHeaders,
      boolean gzip,
      int bufferSize, long maxRequestSize) {
    this.router = router;
    this.factory = factory;
    this.defaultHeaders = defaultHeaders;
    this.gzip = gzip;
    this.bufferSize = bufferSize;
    this.maxRequestSize = maxRequestSize;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    p.addLast("decoder", new HttpRequestDecoder(_4KB, _8KB, bufferSize, false));
    p.addLast("encoder", new HttpResponseEncoder());
    if (gzip) {
      p.addLast("gzip", new HttpContentCompressor());
    }
    p.addLast("handler", new NettyHandler(router, maxRequestSize, bufferSize, factory,
        defaultHeaders));
  }
}
