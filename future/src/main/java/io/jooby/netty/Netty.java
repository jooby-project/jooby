package io.jooby.netty;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.Server;
import io.jooby.Functions;
import io.jooby.internal.netty.NettyMultiHandler;
import io.jooby.internal.netty.NettyNative;
import io.jooby.internal.netty.NettyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Netty implements Server {

  private static class Pipeline extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final ChannelInboundHandler handler;

    public Pipeline(SslContext sslCtx, ChannelInboundHandler handler) {
      this.sslCtx = sslCtx;
      this.handler = handler;
    }

    @Override
    public void initChannel(SocketChannel ch) {
      ChannelPipeline p = ch.pipeline();
      if (sslCtx != null) {
        p.addLast(sslCtx.newHandler(ch.alloc()));
      }
      // FIXME: check configuration parameters
      p.addLast("codec", new HttpServerCodec());
      p.addLast("aggregator", new HttpObjectAggregator(_16KB * 2));
      p.addLast("handler", handler);
    }
  }

  private List<App> applications = new ArrayList<>();

  private boolean SSL = false;

  private EventLoopGroup acceptor;

  private EventLoopGroup ioLoop;

  private int port = 8080;

  private DefaultEventExecutorGroup worker;

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public Server deploy(App application) {
    applications.add(application);
    return this;
  }

  @Nonnull @Override public Server start() {
    try {
      NettyNative provider = NettyNative.get();
      /** Acceptor: */
      this.acceptor = provider.group(1);
      /** Client: */
      this.ioLoop = provider.group(0);

      final SslContext sslCtx;
      if (SSL) {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
      } else {
        sslCtx = null;
      }

      /** Worker: */
      worker = new DefaultEventExecutorGroup(32);

      /** Handler: */
      ChannelInboundHandler handler;
      if (applications.size() == 1) {
        handler = newHandler(applications.get(0), worker);
      } else {
        Map<App, NettyHandler> handlers = new LinkedHashMap<>(applications.size());
        applications.forEach(app -> handlers.put(app, newHandler(app, worker)));
        handler = new NettyMultiHandler(handlers, worker);
      }

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 8192);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new Pipeline(sslCtx, handler))
          .childOption(ChannelOption.SO_REUSEADDR, true);

      bootstrap.bind(port).sync();
    } catch (CertificateException | SSLException x) {
      throw Throwing.sneakyThrow(x);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }

    applications.forEach(app -> app.start(this));

    return this;
  }

  private NettyHandler newHandler(App app, DefaultEventExecutorGroup worker) {
    return app.mode() == Mode.WORKER ?
        new NettyHandler(worker, worker, app) :
        new NettyHandler(null, worker, app);
  }

  public Server stop() {
    try (Functions.Closer closer = Functions.closer()) {
      applications.forEach(app -> closer.register(app::stop));
      closer.register(ioLoop::shutdownGracefully);
      closer.register(acceptor::shutdownGracefully);
      closer.register(worker::shutdownGracefully);
    }
    return this;
  }

}
