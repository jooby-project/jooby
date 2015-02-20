package org.jooby.internal.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.EventExecutorGroup;

import org.jooby.spi.Dispatcher;

import com.typesafe.config.Config;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {

  private EventExecutorGroup executor;

  private Dispatcher dispatcher;

  private Config config;

  public NettyInitializer(final EventExecutorGroup executor, final Dispatcher dispatcher,
      final Config config) {
    this.executor = executor;
    this.dispatcher = dispatcher;
    this.config = config;
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    ch.pipeline()
        .addLast("codec", new HttpServerCodec())
        .addLast("aggregator", new HttpObjectAggregator(512 * 1024))
//        .addLast(new WebSocketServerProtocolHandler("/ws"))
//        .addLast(new TextFrameHandler());
        .addLast(executor, "jooby", new NettyHandler(dispatcher, config));
  }

}
