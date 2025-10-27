/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jetty;

import java.net.BindException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.compression.CompressionPool;
import org.eclipse.jetty.util.compression.DeflaterPool;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Invocable;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.exception.StartupException;
import io.jooby.internal.jetty.JettyHandler;
import io.jooby.internal.jetty.JettyHttpExpectAndContinueHandler;
import io.jooby.internal.jetty.PrefixHandler;
import io.jooby.internal.jetty.http2.JettyHttp2Configurer;
import io.jooby.output.OutputFactory;

/**
 * Web server implementation using <a href="https://www.eclipse.org/jetty/">Jetty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JettyServer extends io.jooby.Server.Base {

  private static final String NAME = "jetty";
  private static final int THREADS = 200;

  static {
    System.setProperty("__server_.workerThreads", THREADS + "");
    System.setProperty("__server_.name", NAME);
  }

  private OutputFactory outputFactory;

  private Server server;

  private ThreadPool threadPool;

  private List<Jooby> applications;

  private Consumer<HttpConfiguration> httpConfigurer;

  /**
   * Creates a jetty web server.
   *
   * @param options Options.
   * @param threadPool Custom thread pool.
   */
  public JettyServer(@NonNull ServerOptions options, @NonNull QueuedThreadPool threadPool) {
    setOptions(options);
    this.threadPool = threadPool;
  }

  /**
   * Creates a jetty web server.
   *
   * @param threadPool Custom thread pool.
   */
  public JettyServer(@NonNull QueuedThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  /**
   * Creates a jetty web server.
   *
   * @param options Options.
   */
  public JettyServer(@NonNull ServerOptions options) {
    setOptions(options);
  }

  /** Creates a default jetty web server. */
  public JettyServer() {}

  @Override
  public OutputFactory getOutputFactory() {
    if (outputFactory == null) {
      this.outputFactory = OutputFactory.create(getOptions().getOutput());
    }
    return outputFactory;
  }

  @Override
  public String getName() {
    return NAME;
  }

  /**
   * Applies custom configuration. This applies to HTTP and HTTPS (when enable).
   *
   * @param configurer Configurer.
   * @return This server.
   */
  public JettyServer configure(Consumer<HttpConfiguration> configurer) {
    this.httpConfigurer = configurer;
    return this;
  }

  @Override
  public io.jooby.Server start(@NonNull Jooby... application) {
    // force options to be non-null
    var options = getOptions();
    var portInUse = options.getPort();
    try {
      this.applications = List.of(application);

      addShutdownHook();

      if (threadPool == null) {
        threadPool = new QueuedThreadPool(options.getWorkerThreads());
        ((QueuedThreadPool) threadPool).setName("worker");
      }

      fireStart(List.of(application), threadPool);

      var acceptors = 1;
      var selectors = options.getIoThreads();
      server = new Server(threadPool);
      server.setStopAtShutdown(false);

      JettyHttp2Configurer http2 =
          options.isHttp2() == Boolean.TRUE ? new JettyHttp2Configurer() : null;

      var httpConf = new HttpConfiguration();
      httpConf.setUriCompliance(UriCompliance.LEGACY);
      httpConf.setOutputBufferSize(options.getOutput().getSize());
      httpConf.setOutputAggregationSize(options.getOutput().getSize());
      httpConf.setSendXPoweredBy(false);
      httpConf.setSendDateHeader(options.getDefaultHeaders());
      httpConf.setSendServerVersion(false);
      httpConf.setRequestHeaderSize(options.getMaxHeaderSize());

      if (httpConfigurer != null) {
        httpConfigurer.accept(httpConf);
      }

      var connectionFactories = new ArrayList<ConnectionFactory>();
      connectionFactories.add(new HttpConnectionFactory(httpConf));
      if (http2 != null) {
        connectionFactories.addAll(http2.configure(httpConf));
      }

      if (!options.isHttpsOnly()) {
        var http =
            new ServerConnector(
                server,
                acceptors,
                selectors,
                connectionFactories.toArray(new ConnectionFactory[0]));
        http.setPort(options.getPort());
        http.setHost(options.getHost());

        server.addConnector(http);
      }

      if (options.isSSLEnabled()) {
        var classLoader = applications.get(0).getClassLoader();
        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSslContext(options.getSSLContext(classLoader));
        var protocol = options.getSsl().getProtocol();
        sslContextFactory.setIncludeProtocols(protocol.toArray(new String[0]));
        // exclude
        isNotInUse(protocol, "TLSv1", sslContextFactory::addExcludeProtocols);
        isNotInUse(protocol, "TLSv1.1", sslContextFactory::addExcludeProtocols);

        var clientAuth =
            Optional.ofNullable(options.getSsl())
                .map(SslOptions::getClientAuth)
                .orElse(SslOptions.ClientAuth.NONE);
        if (clientAuth == SslOptions.ClientAuth.REQUESTED) {
          sslContextFactory.setWantClientAuth(true);
        } else if (clientAuth == SslOptions.ClientAuth.REQUIRED) {
          sslContextFactory.setNeedClientAuth(true);
        }

        var httpsConf = new HttpConfiguration(httpConf);
        httpsConf.addCustomizer(new SecureRequestCustomizer());

        var secureConnectionFactories = new ArrayList<ConnectionFactory>();
        if (http2 == null) {
          secureConnectionFactories.add(new SslConnectionFactory(sslContextFactory, "http/1.1"));
        } else {
          secureConnectionFactories.add(new SslConnectionFactory(sslContextFactory, "alpn"));
          secureConnectionFactories.addAll(http2.configure(httpsConf));
        }
        secureConnectionFactories.add(new HttpConnectionFactory(httpsConf));

        var secureConnector =
            new ServerConnector(
                server,
                acceptors,
                selectors,
                secureConnectionFactories.toArray(new ConnectionFactory[0]));
        portInUse = options.getSecurePort();
        secureConnector.setPort(portInUse);
        secureConnector.setHost(options.getHost());

        server.addConnector(secureConnector);
      } else if (options.isHttpsOnly()) {
        throw new StartupException("Server configured for httpsOnly, but ssl options not set");
      }

      var context = new ContextHandler();
      context.setAttribute(FormFields.MAX_FIELDS_ATTRIBUTE, options.getMaxFormFields());
      context.setAttribute(FormFields.MAX_LENGTH_ATTRIBUTE, options.getMaxRequestSize());
      boolean webSockets =
          application[0].getRoutes().stream().anyMatch(it -> it.getMethod().equals(Router.WS));

      /* ********************************* Compression *************************************/
      var gzip = options.getCompressionLevel() != null;
      var compress = gzip || webSockets;
      if (compress) {
        int compressionLevel =
            Optional.ofNullable(options.getCompressionLevel())
                .orElse(ServerOptions.DEFAULT_COMPRESSION_LEVEL);
        DeflaterPool deflater = newDeflater(compressionLevel);
        server.addBean(deflater, true);
      }

      /* ********************************* Handler *************************************/
      var handlerList = createHandler(options, applications);
      Handler handler =
          handlerList.size() == 1 ? handlerList.get(0).getValue() : new PrefixHandler(handlerList);
      context.setHandler(handler);

      /* ********************************* Gzip *************************************/
      if (gzip) {
        var gzipHandler = new GzipHandler();
        context.insertHandler(gzipHandler);
      }
      /* ********************************* WebSocket *************************************/
      if (webSockets) {
        Config conf = application[0].getConfig();
        int maxSize =
            conf.hasPath("websocket.maxSize")
                ? conf.getBytes("websocket.maxSize").intValue()
                : WebSocket.MAX_BUFFER_SIZE;
        context.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
        long timeout =
            conf.hasPath("websocket.idleTimeout")
                ? conf.getDuration("websocket.idleTimeout", TimeUnit.MILLISECONDS)
                : TimeUnit.MINUTES.toMillis(5);

        var container = ServerWebSocketContainer.ensure(server, context);
        container.setMaxTextMessageSize(maxSize);
        container.setIdleTimeout(Duration.ofMillis(timeout));
      }
      server.setHandler(context);
      server.start();

      fireReady(applications);
    } catch (Exception x) {
      if (io.jooby.Server.isAddressInUse(x.getCause())) {
        x = new BindException("Address already in use: " + portInUse);
      }
      throw SneakyThrows.propagate(x);
    }

    return this;
  }

  private List<Map.Entry<String, Handler>> createHandler(
      ServerOptions options, List<Jooby> applications) {
    return applications.stream()
        .map(
            application -> {
              var invocationType =
                  applications.get(0).getExecutionMode() == ExecutionMode.EVENT_LOOP
                      ? Invocable.InvocationType.NON_BLOCKING
                      : Invocable.InvocationType.BLOCKING;
              Handler handler =
                  new JettyHandler(
                      invocationType,
                      applications.get(0),
                      options.getOutput().getSize(),
                      options.getMaxRequestSize(),
                      options.getDefaultHeaders());
              if (options.isExpectContinue() == Boolean.TRUE) {
                handler = new JettyHttpExpectAndContinueHandler(handler);
              }
              return Map.entry(application.getContextPath(), handler);
            })
        .toList();
  }

  @NonNull @Override
  public List<String> getLoggerOff() {
    return List.of(
        "org.eclipse.jetty.server.Server",
        "org.eclipse.jetty.server.handler.ContextHandler",
        "org.eclipse.jetty.server.AbstractConnector",
        "org.eclipse.jetty.server.Server");
  }

  private DeflaterPool newDeflater(int compressionLevel) {
    ThreadPool.SizedThreadPool threads = server.getBean(ThreadPool.SizedThreadPool.class);
    int capacity = threads == null ? CompressionPool.DEFAULT_CAPACITY : threads.getMaxThreads();
    return new DeflaterPool(capacity, compressionLevel, true);
  }

  private void isNotInUse(List<String> protocols, String protocol, Consumer<String> consumer) {
    if (!protocols.contains(protocol)) {
      consumer.accept(protocol);
    }
  }

  @NonNull @Override
  public synchronized io.jooby.Server stop() {
    if (server != null) {
      fireStop(applications);
      try {
        server.stop();
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      } finally {
        server = null;
      }
    }
    return this;
  }
}
