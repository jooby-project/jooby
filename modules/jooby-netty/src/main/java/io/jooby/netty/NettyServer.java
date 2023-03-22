/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
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
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Web server implementation using <a href="https://netty.io/">Netty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class NettyServer extends Server.Base {

  static {
    System.setProperty(
        "io.netty.leakDetection.level",
        System.getProperty("io.netty.leakDetection.level", "disabled"));
  }

  private static final int _50 = 50;

  private static final int _100 = 100;

  private List<Jooby> applications = new ArrayList<>();

  private EventLoopGroup acceptorloop;

  private EventLoopGroup eventloop;

  private ExecutorService worker;

  private ServerOptions options = new ServerOptions().setServer("netty");

  /**
   * Creates a server.
   *
   * @param worker Thread-pool to use.
   */
  public NettyServer(@NonNull ExecutorService worker) {
    this.worker = worker;
  }

  /** Creates a server. */
  public NettyServer() {}

  @Override
  public NettyServer setOptions(@NonNull ServerOptions options) {
    this.options = options;
    return this;
  }

  @NonNull @Override
  public ServerOptions getOptions() {
    return options;
  }

  @NonNull @Override
  public String getName() {
    return "netty";
  }

  @NonNull @Override
  public Server start(@NonNull Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      /** Worker: Application blocking code */
      if (worker == null) {
        worker =
            Executors.newFixedThreadPool(
                options.getWorkerThreads(), new DefaultThreadFactory("worker"));
      }
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

      boolean http2 = options.isHttp2() == Boolean.TRUE;
      /** Bootstrap: */
      if (!options.isHttpsOnly()) {
        ServerBootstrap http = newBootstrap(transport, newPipeline(factory, null, http2));
        http.bind(options.getHost(), options.getPort()).get();
      }

      if (options.isSSLEnabled()) {
        SSLContext javaSslContext =
            options.getSSLContext(application.getEnvironment().getClassLoader());

        SslOptions sslOptions = options.getSsl();
        String[] protocol = sslOptions.getProtocol().stream().toArray(String[]::new);

        SslOptions.ClientAuth clientAuth = sslOptions.getClientAuth();
        SslContext sslContext = wrap(javaSslContext, toClientAuth(clientAuth), protocol, http2);
        ServerBootstrap https = newBootstrap(transport, newPipeline(factory, sslContext, http2));
        https.bind(options.getHost(), options.getSecurePort()).get();
      } else if (options.isHttpsOnly()) {
        throw new IllegalArgumentException(
            "Server configured for httpsOnly, but ssl options not set");
      }

      fireReady(applications);
    } catch (InterruptedException x) {
      throw SneakyThrows.propagate(x);
    } catch (ExecutionException x) {
      Throwable cause = x.getCause();
      if (Server.isAddressInUse(cause)) {
        cause = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(cause);
    }
    return this;
  }

  private ServerBootstrap newBootstrap(NettyTransport transport, NettyPipeline factory) {
    ServerBootstrap http =
        transport
            .configure(acceptorloop, eventloop)
            .childHandler(factory)
            .childOption(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true);
    return http;
  }

  private ClientAuth toClientAuth(SslOptions.ClientAuth clientAuth) {
    switch (clientAuth) {
      case REQUIRED:
        return ClientAuth.REQUIRE;
      case REQUESTED:
        return ClientAuth.OPTIONAL;
      default:
        return ClientAuth.NONE;
    }
  }

  private NettyPipeline newPipeline(HttpDataFactory factory, SslContext sslContext, boolean http2) {
    return new NettyPipeline(
        acceptorloop.next(),
        applications.get(0),
        factory,
        sslContext,
        http2,
        options.getDefaultHeaders(),
        options.getCompressionLevel(),
        options.getBufferSize(),
        options.getMaxRequestSize(),
        options.isExpectContinue() == Boolean.TRUE);
  }

  @NonNull @Override
  public synchronized Server stop() {
    fireStop(applications);

    shutdown(acceptorloop);
    shutdown(eventloop);
    if (worker != null) {
      worker.shutdown();
      worker = null;
    }
    return this;
  }

  private void shutdown(EventLoopGroup loopGroup) {
    try {
      if (loopGroup != null) {
        acceptorloop.shutdownGracefully().sync();
      }
    } catch (InterruptedException iex) {
      Thread.currentThread().interrupt();
    }
  }

  private SslContext wrap(
      SSLContext sslContext, ClientAuth clientAuth, String[] protocol, boolean http2) {
    ApplicationProtocolConfig protocolConfig;
    if (http2) {
      protocolConfig =
          new ApplicationProtocolConfig(
              ApplicationProtocolConfig.Protocol.ALPN,
              ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
              ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
              Arrays.asList(ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1));
    } else {
      protocolConfig = ApplicationProtocolConfig.DISABLED;
    }
    JdkSslContext jdk =
        new JdkSslContext(
            sslContext,
            false,
            null,
            IdentityCipherSuiteFilter.INSTANCE,
            protocolConfig,
            clientAuth,
            protocol,
            false);

    return jdk;
  }
}
