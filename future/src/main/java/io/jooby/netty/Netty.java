package io.jooby.netty;

import io.jooby.Mode;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.internal.netty.NettyNative;
import io.jooby.internal.netty.NettyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

public class Netty implements Server {

  private static class Pipeline extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final NettyHandler handler;
    private final DefaultEventExecutorGroup worker;

    public Pipeline(SslContext sslCtx, DefaultEventExecutorGroup worker, NettyHandler handler) {
      this.sslCtx = sslCtx;
      this.worker = worker;
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
      p.addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
      p.addLast(worker, "handler", handler);
    }
  }

  private boolean SSL = false;

  private EventLoopGroup acceptor;

  private EventLoopGroup ioLoop;

  private int port = 8080;

  private Mode mode = Mode.WORKER;

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public Server mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  @Nonnull @Override public Server tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public Server start(Router router) {
    try {
      settmpdir(tmpdir);

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
      DefaultEventExecutorGroup worker = new DefaultEventExecutorGroup(32);

      /** Handler: */
      NettyHandler handler = new NettyHandler(worker, router);

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 1024);

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new Pipeline(sslCtx, mode == Mode.WORKER ? worker : null, handler));

      bootstrap.bind(port).sync();
    } catch (CertificateException | SSLException x) {
      throw Throwing.sneakyThrow(x);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
    return this;
  }

  public Server stop() {
    ioLoop.shutdownGracefully();
    acceptor.shutdownGracefully();
    return this;
  }

  private static void settmpdir(Path tmpdir) {
    // should delete file on exit (in normal exit)
    DiskFileUpload.deleteOnExitTemporaryFile = true;
    DiskFileUpload.baseDirectory = tmpdir.toString();
    // should delete file on exit (in normal exit)
    DiskAttribute.deleteOnExitTemporaryFile = true;
    DiskAttribute.baseDirectory = tmpdir.toString();
  }
}
