package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
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
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyServer.class, NioEventLoopGroup.class, DefaultThreadFactory.class,
    DefaultEventExecutorGroup.class, ServerBootstrap.class, SslContextBuilder.class, File.class,
    Epoll.class, EpollEventLoopGroup.class })
public class NettyServerTest {

  Config config = ConfigFactory.empty()
      .withValue("netty.threads.Parent", ConfigValueFactory.fromAnyRef(1))
      .withValue("netty.threads.Max", ConfigValueFactory.fromAnyRef(2))
      .withValue("netty.threads.Name", ConfigValueFactory.fromAnyRef("netty task"))
      .withValue("netty.options.SO_BACKLOG", ConfigValueFactory.fromAnyRef(1024))
      .withValue("netty.child.options.SO_REUSEADDR", ConfigValueFactory.fromAnyRef(true))
      .withValue("netty.http.MaxContentLength", ConfigValueFactory.fromAnyRef("200k"))
      .withValue("netty.http.MaxInitialLineLength", ConfigValueFactory.fromAnyRef("4k"))
      .withValue("netty.http.MaxHeaderSize", ConfigValueFactory.fromAnyRef("8k"))
      .withValue("netty.http.MaxChunkSize", ConfigValueFactory.fromAnyRef("8k"))
      .withValue("netty.http.IdleTimeout", ConfigValueFactory.fromAnyRef("30s"))
      .withValue("netty.channel.CONNECT_TIMEOUT_MILLIS", ConfigValueFactory.fromAnyRef("1s"))
      .withValue("application.port", ConfigValueFactory.fromAnyRef(6789))
      .withValue("application.host", ConfigValueFactory.fromAnyRef("0.0.0.0"));

  private Block parentThreadFactory = unit -> {
    DefaultThreadFactory factory = unit.constructor(DefaultThreadFactory.class)
        .args(String.class, int.class)
        .build("parent", Thread.MAX_PRIORITY);
    unit.registerMock(ThreadFactory.class, factory);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private MockUnit.Block parentEventLoop = unit -> {
    NioEventLoopGroup eventLoop = unit.constructor(NioEventLoopGroup.class)
        .args(int.class, ThreadFactory.class)
        .build(1, unit.get(ThreadFactory.class));
    unit.registerMock(EventLoopGroup.class, eventLoop);
    unit.registerMock(NioEventLoopGroup.class, eventLoop);

    Future future = unit.mock(Future.class);
    expect(eventLoop.shutdownGracefully()).andReturn(future);
    expect(eventLoop.isShutdown()).andReturn(true);
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

  @Test
  public void defaultServer() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void epollServer() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(unit -> {

          unit.mockStatic(Epoll.class);
          expect(Epoll.isAvailable()).andReturn(true);

          EpollEventLoopGroup eventLoop = unit.constructor(EpollEventLoopGroup.class)
              .args(int.class, ThreadFactory.class)
              .build(1, unit.get(ThreadFactory.class));
          unit.registerMock(EventLoopGroup.class, eventLoop);
          unit.registerMock(EpollEventLoopGroup.class, eventLoop);

          Future future = unit.mock(Future.class);
          expect(eventLoop.shutdownGracefully()).andReturn(future);
          expect(eventLoop.isShutdown()).andReturn(true);
        })
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789, EpollEventLoopGroup.class, EpollServerSocketChannel.class))
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @Test
  public void unknownOption() throws Exception {
    Config config = this.config.withValue("netty.options.x", ConfigValueFactory.fromAnyRef(1));
    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void serverWithParentChildEventLoop() throws Exception {
    Config config = this.config.withValue("netty.threads.Child", ConfigValueFactory.fromAnyRef(1));
    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(unit -> {
          NioEventLoopGroup eventLoop = unit.constructor(NioEventLoopGroup.class)
              .args(int.class, ThreadFactory.class)
              .build(1, unit.get(ThreadFactory.class));
          unit.registerMock(EventLoopGroup.class, eventLoop);

          Future future = unit.mock(Future.class);
          expect(eventLoop.shutdownGracefully()).andReturn(future);
        })
        .expect(unit -> {
          DefaultThreadFactory factory = unit.constructor(DefaultThreadFactory.class)
              .args(String.class, int.class)
              .build("child", Thread.MAX_PRIORITY);
          unit.registerMock(DefaultThreadFactory.class, factory);
        })
        .expect(unit -> {
          NioEventLoopGroup eventLoop = unit.constructor(NioEventLoopGroup.class)
              .args(int.class, ThreadFactory.class)
              .build(1, unit.get(DefaultThreadFactory.class));
          unit.registerMock(NioEventLoopGroup.class, eventLoop);

          Future future = unit.mock(Future.class);
          expect(eventLoop.isShutdown()).andReturn(false);
          expect(eventLoop.shutdownGracefully()).andReturn(future);
        })
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @Test
  public void httpsServer() throws Exception {
    Config config = this.config
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443))
        .withValue("ssl.keystore.cert", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"))
        .withValue("ssl.keystore.key", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.key"));

    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .expect(bootstrap(8443))
        .expect(unit -> {
          unit.mockStatic(SslContextBuilder.class);

          SslContext sslCtx = unit.mock(SslContext.class);

          SslContextBuilder scb = unit.mock(SslContextBuilder.class);
          expect(scb.build()).andReturn(sslCtx);

          expect(SslContextBuilder.forServer(Paths.get("target", "unsecure.crt").toFile(),
              Paths.get("target", "unsecure.key").toFile(), null)).andReturn(scb);
        })
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @Test
  public void httpsServerWithKeyPassword() throws Exception {
    Config config = this.config
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443))
        .withValue("ssl.keystore.cert", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"))
        .withValue("ssl.keystore.key", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.key"))
        .withValue("ssl.keystore.password", ConfigValueFactory.fromAnyRef("password"));

    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .expect(bootstrap(8443))
        .expect(unit -> {
          unit.mockStatic(SslContextBuilder.class);

          SslContext sslCtx = unit.mock(SslContext.class);

          SslContextBuilder scb = unit.mock(SslContextBuilder.class);
          expect(scb.build()).andReturn(sslCtx);

          expect(SslContextBuilder.forServer(Paths.get("target", "unsecure.crt").toFile(),
              Paths.get("target", "unsecure.key").toFile(), "password")).andReturn(scb);
        })
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @Test
  public void httpsServerWithTrustCert() throws Exception {
    Config config = this.config
        .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
        .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443))
        .withValue("ssl.keystore.cert", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"))
        .withValue("ssl.keystore.key", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.key"))
        .withValue("ssl.trust.cert", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"));

    new MockUnit(HttpHandler.class)
        .expect(parentThreadFactory)
        .expect(noepoll)
        .expect(parentEventLoop)
        .expect(taskThreadFactory)
        .expect(taskExecutor)
        .expect(channel)
        .expect(bootstrap(6789))
        .expect(bootstrap(8443))
        .expect(unit -> {
          unit.mockStatic(SslContextBuilder.class);

          SslContext sslCtx = unit.mock(SslContext.class);

          SslContextBuilder scb = unit.mock(SslContextBuilder.class);
          expect(scb.trustManager(Paths.get("target", "unsecure.crt").toFile())).andReturn(scb);
          expect(scb.build()).andReturn(sslCtx);

          expect(SslContextBuilder.forServer(Paths.get("target", "unsecure.crt").toFile(),
              Paths.get("target", "unsecure.key").toFile(), null)).andReturn(scb);
        })
        .run(unit -> {
          NettyServer server = new NettyServer(unit.get(HttpHandler.class), config);
          try {
            server.start();
            server.join();
          } finally {
            server.stop();
          }
        });
  }

  @Test
  public void shouldNotCopyFileIfPresent() throws Exception {
    assertNotNull(NettyServer.toFile(Paths.get("pom.xml").toString(), "target"));
  }

  @Test
  public void shouldCopyCpFile() throws Exception {
    assertNotNull(NettyServer.toFile(Paths.get("org/jooby/unsecure.crt").toString(), "target"));
  }

  @Test(expected = FileNotFoundException.class)
  public void shouldFailOnMissingFile() throws Exception {
    assertNotNull(NettyServer.toFile(Paths.get("org/jooby/missing.crt").toString(), "target"));
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
      expect(bootstrap.childHandler(isA(NettyInitializer.class))).andReturn(bootstrap);

      expect(bootstrap.option(ChannelOption.SO_BACKLOG, 1024)).andReturn(bootstrap);
      expect(bootstrap.childOption(ChannelOption.SO_REUSEADDR, true)).andReturn(bootstrap);

      ChannelFuture future = unit.mock(ChannelFuture.class);
      expect(future.sync()).andReturn(future);
      expect(future.channel()).andReturn(unit.get(Channel.class));

      expect(bootstrap.bind("0.0.0.0", port)).andReturn(future);

      unit.registerMock(ServerBootstrap.class, bootstrap);
    };
  }

}
