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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.compression.CompressionPool;
import org.eclipse.jetty.util.compression.DeflaterPool;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
import io.jooby.WebSocket;
import io.jooby.internal.jetty.JettyServlet;
import io.jooby.internal.jetty.http2.JettyHttp2Configurer;

/**
 * Web server implementation using <a href="https://www.eclipse.org/jetty/">Jetty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JettyServer extends io.jooby.Server.Base {

  private static final int THREADS = 200;

  private Server server;

  private ThreadPool threadPool;
  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options = new ServerOptions().setServer("jetty").setWorkerThreads(THREADS);

  public JettyServer(@NonNull ThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  public JettyServer() {}

  @NonNull @Override
  public JettyServer setOptions(@NonNull ServerOptions options) {
    this.options = options.setWorkerThreads(options.getWorkerThreads(THREADS));
    return this;
  }

  @NonNull @Override
  public ServerOptions getOptions() {
    return options;
  }

  @NonNull @Override
  public String getName() {
    return "jetty";
  }

  @NonNull @Override
  public io.jooby.Server start(Jooby application) {
    try {
      /** Set max request size attribute: */
      System.setProperty(
          "org.eclipse.jetty.server.Request.maxFormContentSize",
          Long.toString(options.getMaxRequestSize()));

      applications.add(application);

      addShutdownHook();

      if (threadPool == null) {
        threadPool = new QueuedThreadPool(options.getWorkerThreads());
        ((QueuedThreadPool) threadPool).setName("worker");
      }

      fireStart(applications, threadPool);

      this.server = new Server(threadPool);
      server.setStopAtShutdown(false);

      JettyHttp2Configurer http2 =
          options.isHttp2() == Boolean.TRUE ? new JettyHttp2Configurer() : null;

      HttpConfiguration httpConf = new HttpConfiguration();
      // TODO: we might need to remove legacy with default
      httpConf.setUriCompliance(UriCompliance.LEGACY);
      httpConf.setOutputBufferSize(options.getBufferSize());
      httpConf.setOutputAggregationSize(options.getBufferSize());
      httpConf.setSendXPoweredBy(false);
      httpConf.setSendDateHeader(options.getDefaultHeaders());
      httpConf.setSendServerVersion(false);
      httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);

      List<ConnectionFactory> connectionFactories = new ArrayList<>();
      connectionFactories.add(new HttpConnectionFactory(httpConf));
      if (http2 != null) {
        connectionFactories.addAll(http2.configure(httpConf));
      }

      if (!options.isHttpsOnly()) {
        ServerConnector http =
            new ServerConnector(server, connectionFactories.toArray(new ConnectionFactory[0]));
        http.setPort(options.getPort());
        http.setHost(options.getHost());

        server.addConnector(http);
      }

      if (options.isSSLEnabled()) {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSslContext(
            options.getSSLContext(application.getEnvironment().getClassLoader()));
        List<String> protocol = options.getSsl().getProtocol();
        sslContextFactory.setIncludeProtocols(protocol.toArray(new String[0]));
        // exclude
        isNotInUse(protocol, "TLSv1", sslContextFactory::addExcludeProtocols);
        isNotInUse(protocol, "TLSv1.1", sslContextFactory::addExcludeProtocols);

        SslOptions.ClientAuth clientAuth =
            Optional.ofNullable(options.getSsl())
                .map(SslOptions::getClientAuth)
                .orElse(SslOptions.ClientAuth.NONE);
        if (clientAuth == SslOptions.ClientAuth.REQUESTED) {
          sslContextFactory.setWantClientAuth(true);
        } else if (clientAuth == SslOptions.ClientAuth.REQUIRED) {
          sslContextFactory.setNeedClientAuth(true);
        }

        HttpConfiguration httpsConf = new HttpConfiguration(httpConf);
        httpsConf.addCustomizer(new SecureRequestCustomizer());

        List<ConnectionFactory> secureConnectionFactories = new ArrayList<>();
        if (http2 == null) {
          secureConnectionFactories.add(new SslConnectionFactory(sslContextFactory, "http/1.1"));
        } else {
          secureConnectionFactories.add(new SslConnectionFactory(sslContextFactory, "alpn"));
          http2.configure(httpsConf).forEach(secureConnectionFactories::add);
        }
        secureConnectionFactories.add(new HttpConnectionFactory(httpsConf));

        ServerConnector secureConnector =
            new ServerConnector(
                server, secureConnectionFactories.toArray(new ConnectionFactory[0]));
        secureConnector.setPort(options.getSecurePort());
        secureConnector.setHost(options.getHost());

        server.addConnector(secureConnector);
      } else if (options.isHttpsOnly()) {
        throw new IllegalArgumentException(
            "Server configured for httpsOnly, but ssl options not set");
      }

      ServletContextHandler context = new ServletContextHandler();

      boolean webSockets =
          application.getRoutes().stream().anyMatch(it -> it.getMethod().equals(Router.WS));

      /* ********************************* Compression *************************************/
      boolean gzip = options.getCompressionLevel() != null;
      boolean compress = gzip || webSockets;
      if (compress) {
        int compressionLevel =
            Optional.ofNullable(options.getCompressionLevel())
                .orElse(ServerOptions.DEFAULT_COMPRESSION_LEVEL);
        DeflaterPool deflater = newDeflater(compressionLevel);
        server.addBean(deflater, true);
      }

      /* ********************************* Servlet *************************************/
      JettyServlet servlet =
          new JettyServlet(
              applications.get(0),
              options.getBufferSize(),
              options.getMaxRequestSize(),
              options.getDefaultHeaders());
      context.addServlet(new ServletHolder(servlet), "/*");

      /* ********************************* Gzip *************************************/
      if (gzip) {
        DeflaterPool deflater = server.getBean(DeflaterPool.class);

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setDeflaterPool(deflater);

        context.insertHandler(gzipHandler);
      }
      /* ********************************* WebSocket *************************************/
      if (webSockets) {
        Config conf = application.getConfig();
        int maxSize =
            conf.hasPath("websocket.maxSize")
                ? conf.getBytes("websocket.maxSize").intValue()
                : WebSocket.MAX_BUFFER_SIZE;
        context.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
        long timeout =
            conf.hasPath("websocket.idleTimeout")
                ? conf.getDuration("websocket.idleTimeout", TimeUnit.MILLISECONDS)
                : TimeUnit.MINUTES.toMillis(5);

        JettyWebSocketServletContainerInitializer.configure(
            context,
            (servletContext, container) -> {
              container.setMaxTextMessageSize(maxSize);
              container.setIdleTimeout(Duration.ofMillis(timeout));
            });
      }

      server.setHandler(context);
      server.start();

      fireReady(applications);
    } catch (Exception x) {
      if (io.jooby.Server.isAddressInUse(x.getCause())) {
        x = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(x);
    }

    return this;
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
    fireStop(applications);
    if (server != null) {
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
