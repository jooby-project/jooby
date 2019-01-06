package io.jooby.internal.netty;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
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
    // FIXME: check configuration parameters
    p.addLast("codec", new HttpServerCodec());
    // p.addLast("aggregator", new HttpObjectAggregator((int) maxRequestSize));
    if (gzip) {
      p.addLast("gzip", new HttpContentCompressor());
    }
    p.addLast("handler", handler.get());
  }
}
