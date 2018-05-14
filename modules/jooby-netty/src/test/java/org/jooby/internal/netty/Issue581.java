package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.io.File;
import java.util.concurrent.ThreadFactory;

import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyServer.class, NioEventLoopGroup.class, DefaultThreadFactory.class,
    DefaultEventExecutorGroup.class, ServerBootstrap.class, SslContextBuilder.class, File.class,
    Epoll.class, EpollEventLoopGroup.class, NettySslContext.class })
public class Issue581 {

  Config config = ConfigFactory.empty()
      .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(false))
      .withValue("netty.threads.Boss", ConfigValueFactory.fromAnyRef(1))
      .withValue("netty.threads.Worker", ConfigValueFactory.fromAnyRef(0))
      .withValue("netty.threads.Max", ConfigValueFactory.fromAnyRef(2))
      .withValue("netty.threads.Name", ConfigValueFactory.fromAnyRef("netty task"))
      .withValue("netty.options.SO_BACKLOG", ConfigValueFactory.fromAnyRef(1024))
      .withValue("netty.worker.options.SO_REUSEADDR", ConfigValueFactory.fromAnyRef(true))
      .withValue("netty.http.MaxContentLength", ConfigValueFactory.fromAnyRef("200k"))
      .withValue("netty.http.MaxInitialLineLength", ConfigValueFactory.fromAnyRef("4k"))
      .withValue("netty.http.MaxHeaderSize", ConfigValueFactory.fromAnyRef("8k"))
      .withValue("netty.http.MaxChunkSize", ConfigValueFactory.fromAnyRef("8k"))
      .withValue("netty.http.IdleTimeout", ConfigValueFactory.fromAnyRef("30s"))
      .withValue("netty.options.CONNECT_TIMEOUT_MILLIS", ConfigValueFactory.fromAnyRef(1000))
      .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
      .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"))
      .withValue("server.http.ResponseBufferSize", ConfigValueFactory.fromAnyRef("16k"))
      .withValue("server.ws.MaxTextMessageSize", ConfigValueFactory.fromAnyRef("16k"))
      .withValue("server.ws.MaxBinaryMessageSize", ConfigValueFactory.fromAnyRef("16k"))
      .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"));

  @SuppressWarnings({"rawtypes", "unchecked" })
  private MockUnit.Block parentEventLoop = unit -> {
    NioEventLoopGroup eventLoop = unit.constructor(NioEventLoopGroup.class)
        .args(int.class, ThreadFactory.class)
        .build(1, unit.get(ThreadFactory.class));
    unit.registerMock(EventLoopGroup.class, eventLoop);
    unit.registerMock(NioEventLoopGroup.class, eventLoop);

    Future future = unit.mock(Future.class);
    unit.registerMock(Future.class, future);
    expect(future.isSuccess()).andReturn(true).times(2);
    expect(future.addListener(unit.capture(GenericFutureListener.class))).andReturn(future)
        .times(2);
    expect(eventLoop.shutdownGracefully()).andReturn(future).times(2);
    expect(eventLoop.isShuttingDown()).andReturn(false).times(2);
  };

  private Block taskThreadFactory = unit -> {
    DefaultThreadFactory factory = unit.constructor(DefaultThreadFactory.class)
        .args(String.class)
        .build("netty task");
    unit.registerMock(DefaultThreadFactory.class, factory);
  };

  private Block taskExecutor = unit -> {
    DefaultEventExecutorGroup executor = unit.constructor(DefaultEventExecutorGroup.class)
        .args(int.class, ThreadFactory.class)
        .build(2, unit.get(DefaultThreadFactory.class));
    unit.registerMock(EventExecutorGroup.class, executor);
  };

  private Block channel = unit -> {
    ChannelFuture cfuture = unit.mock(ChannelFuture.class);
    expect(cfuture.sync()).andReturn(cfuture);

    Channel channel = unit.mock(Channel.class);
    expect(channel.closeFuture()).andReturn(cfuture);
    unit.registerMock(Channel.class, channel);
  };

  private Block noepoll = unit -> {
    unit.mockStatic(Epoll.class);
    expect(Epoll.isAvailable()).andReturn(false).times(1, 2);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void shouldShutdownExecutor() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory("nio-boss"))
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .expect(unit -> {
          EventExecutorGroup executor = unit.get(EventExecutorGroup.class);
          Future future = unit.mock(Future.class);
          expect(executor.shutdownGracefully()).andReturn(future);
          expect(executor.isShuttingDown()).andReturn(false);
          expect(future.addListener(unit.capture(GenericFutureListener.class))).andReturn(future);
        })
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        }, unit -> {
          unit.captured(GenericFutureListener.class).get(0)
              .operationComplete(unit.get(Future.class));
          unit.captured(GenericFutureListener.class).get(0)
              .operationComplete(unit.get(Future.class));
        });
  }

  private Block parentThreadFactory(final String name) {
    return unit -> {
      DefaultThreadFactory factory = unit.constructor(DefaultThreadFactory.class)
          .args(String.class, int.class)
          .build(name, false);
      unit.registerMock(ThreadFactory.class, factory);
    };
  }

  private Block bootstrap(final int port) {
    return bootstrap(port, NioEventLoopGroup.class, NioServerSocketChannel.class);
  }

  private Block bootstrap(final int port, final Class<? extends EventLoopGroup> eventLoop,
      final Class<? extends ServerChannel> channelClass) {
    return unit -> {
      ServerBootstrap bootstrap = unit.mockConstructor(ServerBootstrap.class);

      expect(bootstrap.group(
          unit.get(EventLoopGroup.class), unit.get(eventLoop)))
              .andReturn(bootstrap);

      LoggingHandler handler = unit.constructor(LoggingHandler.class)
          .args(Class.class, LogLevel.class)
          .build(Server.class, LogLevel.DEBUG);

      expect(bootstrap.channel(channelClass)).andReturn(bootstrap);
      expect(bootstrap.handler(handler)).andReturn(bootstrap);
      expect(bootstrap.childHandler(isA(NettyPipeline.class))).andReturn(bootstrap);

      expect(bootstrap.option(ChannelOption.SO_BACKLOG, 1024)).andReturn(bootstrap);
      expect(bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)).andReturn(bootstrap);
      expect(bootstrap.childOption(ChannelOption.SO_REUSEADDR, true)).andReturn(bootstrap);

      ChannelFuture future = unit.mock(ChannelFuture.class);
      expect(future.sync()).andReturn(future);
      expect(future.channel()).andReturn(unit.get(Channel.class));

      expect(bootstrap.bind("0.0.0.0", port)).andReturn(future);

      unit.registerMock(ServerBootstrap.class, bootstrap);
    };
  }

}
