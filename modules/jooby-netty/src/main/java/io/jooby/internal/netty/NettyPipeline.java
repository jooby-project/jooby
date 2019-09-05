/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.Router;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.multipart.HttpDataFactory;

import java.util.concurrent.ScheduledExecutorService;

import static io.jooby.ServerOptions._4KB;
import static io.jooby.ServerOptions._8KB;

public class NettyPipeline extends ChannelInitializer<SocketChannel> {

  private final Router router;
  private final HttpDataFactory factory;
  private final boolean gzip;
  private final int bufferSize;
  private final long maxRequestSize;
  private final boolean defaultHeaders;
  private final ScheduledExecutorService service;

  public NettyPipeline(ScheduledExecutorService service, Router router, HttpDataFactory factory,
      boolean defaultHeaders, boolean gzip, int bufferSize, long maxRequestSize) {
    this.service = service;
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
    p.addLast("encoder", new HttpResponseEncoder());
    p.addLast("decoder", new HttpRequestDecoder(_4KB, _8KB, bufferSize, false));
    if (gzip) {
      p.addLast("gzip", new HttpContentCompressor());
    }
    p.addLast("handler", new NettyHandler(service, router, maxRequestSize, bufferSize, factory,
        defaultHeaders));
  }
}
