/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import static io.jooby.ServerOptions._4KB;
import static io.jooby.internal.netty.NettyHeadersFactory.HEADERS;
import static java.util.concurrent.Executors.newFixedThreadPool;

import java.net.BindException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import javax.net.ssl.SSLContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.*;
import io.jooby.exception.StartupException;
import io.jooby.internal.netty.*;
import io.jooby.output.OutputFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Web server implementation using <a href="https://netty.io/">Netty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class NettyServer extends Server.Base {

  private static final String NAME = "netty";
  private static final String LEAK_DETECTION = "io.netty.leakDetection.level";

  static {
    System.setProperty("__server_.name", NAME);
    System.setProperty(
        LEAK_DETECTION,
        System.getProperty(LEAK_DETECTION, ResourceLeakDetector.Level.DISABLED.name()));
    ResourceLeakDetector.setLevel(
        ResourceLeakDetector.Level.valueOf(System.getProperty(LEAK_DETECTION)));
  }

  private NettyEventLoopGroup eventLoop;
  private ExecutorService worker;

  private List<Jooby> applications;
  private NettyOutputFactory outputFactory;
  private boolean singleEventLoopGroup =
      System.getProperty("io.netty.eventLoopGroup", "parent-child").equals("single");

  /**
   * Creates a server.
   *
   * @param worker Thread-pool to use.
   */
  public NettyServer(@NonNull ServerOptions options, @NonNull ExecutorService worker) {
    super.setOptions(options);
    this.worker = worker;
  }

  /**
   * Creates a server.
   *
   * @param worker Thread-pool to use.
   */
  public NettyServer(@NonNull ExecutorService worker) {
    this.worker = worker;
  }

  /** Creates a server. */
  public NettyServer(@NonNull ServerOptions options) {
    super.setOptions(options);
    options.setServer(NAME);
  }

  public NettyServer() {}

  /**
   * Configure netty to use a single event loop group. When true, the acceptor and eventLoop share
   * the same {@link EventLoopGroup}. Default is <code>true</code>.
   *
   * @param value True for shared {@link EventLoopGroup}.
   * @return This server.
   */
  public NettyServer setSingleEventLoopGroup(boolean value) {
    this.singleEventLoopGroup = value;
    return this;
  }

  @Override
  public OutputFactory getOutputFactory() {
    if (outputFactory == null) {
      outputFactory = new NettyOutputFactory(ByteBufAllocator.DEFAULT, getOptions().getOutput());
    }
    return outputFactory;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Server start(@NonNull Jooby... application) {
    // force options to be non-null
    var options = getOptions();
    var portInUse = options.getPort();
    try {
      this.applications = List.of(application);
      /* Worker: Application blocking code */
      if (worker == null) {
        worker = newFixedThreadPool(options.getWorkerThreads(), new DefaultThreadFactory("worker"));
      }
      // Make sure context use same buffer factory
      for (var app : applications) {
        app.getServices().put(ServerOptions.class, options);
        app.getServices().put(Server.class, this);
      }

      addShutdownHook();

      fireStart(List.of(application), worker);

      var classLoader = applications.get(0).getClassLoader();

      var transport = NettyTransport.transport(classLoader);
      eventLoop = createEventLoopGroup();
      if (eventLoop == null) {
        eventLoop =
            new NettyEventLoopGroupImpl(transport, singleEventLoopGroup, options.getIoThreads());
      }

      var outputFactory = (NettyOutputFactory) getOutputFactory();
      var allocator = outputFactory.getAllocator();
      var http2 = options.isHttp2() == Boolean.TRUE;
      /* Bootstrap: */
      if (!options.isHttpsOnly()) {
        var http = newBootstrap(allocator, transport, newPipeline(options, null, http2), eventLoop);
        http.bind(options.getHost(), options.getPort()).get();
      }

      if (options.isSSLEnabled()) {
        var javaSslContext = options.getSSLContext(classLoader);

        var sslOptions = options.getSsl();
        var protocol = sslOptions.getProtocol().toArray(String[]::new);

        var clientAuth = sslOptions.getClientAuth();
        var sslContext = wrap(javaSslContext, toClientAuth(clientAuth), protocol, http2);
        var https =
            newBootstrap(allocator, transport, newPipeline(options, sslContext, http2), eventLoop);
        portInUse = options.getSecurePort();
        https.bind(options.getHost(), portInUse).get();
      } else if (options.isHttpsOnly()) {
        throw new StartupException("Server configured for httpsOnly, but ssl options not set");
      }

      fireReady(applications);
    } catch (InterruptedException x) {
      throw SneakyThrows.propagate(x);
    } catch (ExecutionException x) {
      var cause = x.getCause();
      if (Server.isAddressInUse(cause)) {
        cause = new BindException("Address already in use: " + portInUse);
      }
      throw SneakyThrows.propagate(cause);
    }
    return this;
  }

  protected @Nullable NettyEventLoopGroup createEventLoopGroup() {
    return null;
  }

  private ServerBootstrap newBootstrap(
      ByteBufAllocator allocator,
      NettyTransport transport,
      NettyPipeline factory,
      NettyEventLoopGroup group) {
    return transport
        .configure(group.getParent(), group.getChild())
        .childHandler(factory)
        .childOption(ChannelOption.ALLOCATOR, allocator)
        .childOption(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.TCP_NODELAY, true);
  }

  private ClientAuth toClientAuth(SslOptions.ClientAuth clientAuth) {
    return switch (clientAuth) {
      case REQUIRED -> ClientAuth.REQUIRE;
      case REQUESTED -> ClientAuth.OPTIONAL;
      default -> ClientAuth.NONE;
    };
  }

  private NettyPipeline newPipeline(ServerOptions options, SslContext sslContext, boolean http2) {
    var decoderConfig =
        new HttpDecoderConfig()
            .setMaxInitialLineLength(_4KB)
            .setMaxHeaderSize(options.getMaxHeaderSize())
            .setMaxChunkSize(options.getOutput().getSize())
            .setHeadersFactory(HEADERS)
            .setTrailersFactory(HEADERS);
    return new NettyPipeline(
        sslContext,
        decoderConfig,
        Context.Selector.create(applications),
        options.getMaxRequestSize(),
        options.getOutput().getSize(),
        options.getDefaultHeaders(),
        http2,
        options.isExpectContinue() == Boolean.TRUE,
        options.getCompressionLevel());
  }

  @Override
  public synchronized Server stop() {
    fireStop(applications);
    // only for jooby build where close events may take longer.
    NettyWebSocket.all.clear();

    eventLoop.shutdown();
    if (worker != null) {
      worker.shutdown();
      worker = null;
    }
    return this;
  }

  private void shutdown(EventLoopGroup eventLoopGroup, int quietPeriod) {
    if (eventLoopGroup != null) {
      eventLoopGroup.shutdownGracefully(quietPeriod, 15, TimeUnit.SECONDS);
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
    return new JdkSslContext(
        sslContext,
        false,
        null,
        IdentityCipherSuiteFilter.INSTANCE,
        protocolConfig,
        clientAuth,
        protocol,
        false);
  }
}
