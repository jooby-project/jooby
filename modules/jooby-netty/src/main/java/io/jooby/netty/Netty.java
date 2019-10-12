/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.internal.netty.NettyPipeline;
import io.jooby.internal.netty.NettyTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Web server implementation using <a href="https://netty.io/">Netty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Netty extends Server.Base {

  static {
    System.setProperty("io.netty.leakDetection.level",
        System.getProperty("io.netty.leakDetection.level", "disabled"));
  }

  private static final int _50 = 50;

  private static final int _100 = 100;

  private List<Jooby> applications = new ArrayList<>();

  private EventLoopGroup acceptorloop;

  private EventLoopGroup eventloop;

  private ExecutorService worker;

  private ServerOptions options = new ServerOptions()
      .setServer("netty");

  @Override public Netty setOptions(@Nonnull ServerOptions options) {
    this.options = options;
    return this;
  }

  @Nonnull @Override public ServerOptions getOptions() {
    return options;
  }

  @Nonnull @Override public Server start(@Nonnull Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      /** Worker: Application blocking code */
      worker = Executors.newFixedThreadPool(
          options.getWorkerThreads(),
          new DefaultThreadFactory("worker")
      );
      fireStart(applications, worker);

      /** Disk attributes: */
      String tmpdir = applications.get(0).getTmpdir().toString();
      DiskFileUpload.baseDirectory = tmpdir;
      DiskAttribute.baseDirectory = tmpdir;

      NettyTransport transport = NettyTransport.transport(application.getClassLoader());

      /** Acceptor event-loop */
      this.acceptorloop = transport.createEventLoop(1, "acceptor", _50);

      /** Event loop: processing connections, parsing messages and doing engine's internal work */
      this.eventloop = transport.createEventLoop(options.getIoThreads(), "eventloop", _100);

      /** File data factory: */
      HttpDataFactory factory = new DefaultHttpDataFactory(options.getBufferSize());

      /** Bootstrap: */
      ServerBootstrap http = transport.configure(acceptorloop, eventloop)
          .childHandler(newPipeline(factory, null))
          .childOption(ChannelOption.SO_REUSEADDR, true)
          .childOption(ChannelOption.TCP_NODELAY, true);

      http.bind(options.getHost(), options.getPort()).get();

      if (options.isSSLEnabled()) {
        ServerBootstrap https = transport.configure(acceptorloop, eventloop)
            .childHandler(newPipeline(factory, options.getSSLContext()))
            .childOption(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true);

        https.bind(options.getHost(), options.getSecurePort()).get();
      }

      fireReady(applications);
    } catch (InterruptedException x) {
      throw SneakyThrows.propagate(x);
    } catch (ExecutionException x) {
      Throwable cause = x.getCause();
      if (cause instanceof BindException) {
        cause = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(cause);
    }
    return this;
  }

  private NettyPipeline newPipeline(HttpDataFactory factory, SSLContext sslContext) {
    return new NettyPipeline(
            acceptorloop.next(),
            applications.get(0),
            factory,
            wrap(sslContext),
            options.getDefaultHeaders(),
            options.getGzip(),
            options.getBufferSize(),
            options.getMaxRequestSize()
        );
  }

  @Nonnull @Override public synchronized Server stop() {
    fireStop(applications);
    if (acceptorloop != null) {
      acceptorloop.shutdownGracefully();
      acceptorloop = null;
    }
    if (eventloop != null) {
      eventloop.shutdownGracefully();
      eventloop = null;
    }
    if (worker != null) {
      worker.shutdown();
      worker = null;
    }
    return this;
  }

  private SslContext wrap(SSLContext sslContext) {
    if (sslContext != null) {
      return new JdkSslContext(sslContext, false, null, IdentityCipherSuiteFilter.INSTANCE,
          ApplicationProtocolConfig.DISABLED, ClientAuth.NONE, null, false);
    }
    return null;
  }
}
