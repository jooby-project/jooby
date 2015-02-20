package org.jooby.internal.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.inject.Inject;

import org.jooby.spi.Dispatcher;
import org.jooby.spi.Server;

import com.typesafe.config.Config;

public class NettyServer implements Server {

  private NioEventLoopGroup bossGroup;

  private NioEventLoopGroup workerGroup;

  private ChannelFuture closeFuture;

  private Config config;

  private NettyInitializer initializer;

  @Inject
  public NettyServer(final Dispatcher dispatcher, final Config config) {
    this.bossGroup = new NioEventLoopGroup();
    this.workerGroup = new NioEventLoopGroup();
    this.initializer = new NettyInitializer(workerGroup, dispatcher, config);
    this.config = config;
  }

  @Override
  public void start() throws Exception {
    ServerBootstrap bootstrap = new ServerBootstrap();

    bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
//        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(initializer);

    Channel ch = bootstrap.bind(config.getInt("application.port")).sync().channel();

    closeFuture = ch.closeFuture();
  }

  @Override
  public void stop() throws Exception {
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  @Override
  public void join() throws InterruptedException {
    closeFuture.sync();
  }

}
