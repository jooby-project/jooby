/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jetty;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

import com.typesafe.config.Config;
import io.jooby.Http2Configurer;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.SslOptions;
import io.jooby.WebSocket;
import io.jooby.internal.jetty.JettyHandler;
import io.jooby.internal.jetty.JettyWebSocket;

/**
 * Web server implementation using <a href="https://www.eclipse.org/jetty/">Jetty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Jetty extends io.jooby.Server.Base {

  private static final int THREADS = 200;

  private Server server;

  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options = new ServerOptions()
      .setServer("jetty")
      .setWorkerThreads(THREADS);

  static {
    System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
  }

  @Nonnull @Override public Jetty setOptions(@Nonnull ServerOptions options) {
    this.options = options
        .setWorkerThreads(options.getWorkerThreads(THREADS));
    return this;
  }

  @Nonnull @Override public ServerOptions getOptions() {
    return options;
  }

  @Nonnull @Override public io.jooby.Server start(Jooby application) {
    try {
      System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset", "utf-8");
      /** Set max request size attribute: */
      System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
          Long.toString(options.getMaxRequestSize()));

      applications.add(application);

      addShutdownHook();

      QueuedThreadPool executor = new QueuedThreadPool(options.getWorkerThreads());
      executor.setName("worker");

      fireStart(applications, executor);

      this.server = new Server(executor);
      server.setStopAtShutdown(false);

      Http2Configurer<HttpConfiguration, List<ConnectionFactory>> http2;
      if (options.isHttp2() == null || options.isHttp2() == Boolean.TRUE) {
        http2 = stream(spliteratorUnknownSize(
            ServiceLoader.load(Http2Configurer.class).iterator(),
            Spliterator.ORDERED),
            false
        )
            .filter(it -> it.support(HttpConfiguration.class))
            .findFirst()
            .orElse(null);
      } else {
        http2 = null;
      }

      HttpConfiguration httpConf = new HttpConfiguration();
      httpConf.setOutputBufferSize(options.getBufferSize());
      httpConf.setOutputAggregationSize(options.getBufferSize());
      httpConf.setSendXPoweredBy(false);
      httpConf.setSendDateHeader(options.getDefaultHeaders());
      httpConf.setSendServerVersion(false);
      httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);

      List<ConnectionFactory> connectionFactories = new ArrayList<>();
      connectionFactories.add(new HttpConnectionFactory(httpConf));
      Optional.ofNullable(http2)
          .ifPresent(extension -> connectionFactories.addAll(extension.configure(httpConf)));

      ServerConnector http = new ServerConnector(server,
          connectionFactories.toArray(new ConnectionFactory[connectionFactories.size()]));
      http.setPort(options.getPort());
      http.setHost(options.getHost());

      server.addConnector(http);

      if (options.isSSLEnabled()) {
        String provider = http2 == null ? null : "Conscrypt";
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory
            .setSslContext(options.getSSLContext(application.getEnvironment().getClassLoader(), provider));
        List<String> protocol = options.getSsl().getProtocol();
        sslContextFactory.setIncludeProtocols(protocol.toArray(new String[protocol.size()]));
        // exclude
        isNotInUse(protocol, "TLSv1", sslContextFactory::addExcludeProtocols);
        isNotInUse(protocol, "TLSv1.1", sslContextFactory::addExcludeProtocols);

        SslOptions.ClientAuth clientAuth = Optional.ofNullable(options.getSsl())
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

        ServerConnector secureConnector = new ServerConnector(server,
            secureConnectionFactories
                .toArray(new ConnectionFactory[secureConnectionFactories.size()]));
        secureConnector.setPort(options.getSecurePort());
        secureConnector.setHost(options.getHost());

        server.addConnector(secureConnector);
      }

      ContextHandler context = new ContextHandler();

      AbstractHandler handler = new JettyHandler(applications.get(0), options.getBufferSize(),
          options.getMaxRequestSize(), options.getDefaultHeaders());

      if (options.getCompressionLevel() != null) {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setCompressionLevel(options.getCompressionLevel());
        gzipHandler.setHandler(handler);
        context.setHandler(gzipHandler);
      } else {
        context.setHandler(handler);
      }
      /* ********************************* WebSocket *************************************/
      context.setAttribute(DecoratedObjectFactory.ATTR, new DecoratedObjectFactory());
      WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
      policy.setMaxTextMessageBufferSize(WebSocket.MAX_BUFFER_SIZE);
      policy.setMaxTextMessageSize(WebSocket.MAX_BUFFER_SIZE);
      Config conf = application.getConfig();
      long timeout = conf.hasPath("websocket.idleTimeout")
          ? conf.getDuration("websocket.idleTimeout", TimeUnit.MINUTES)
          : 5;
      policy.setIdleTimeout(TimeUnit.MINUTES.toMillis(timeout));
      WebSocketServerFactory wssf = new WebSocketServerFactory(context.getServletContext(), policy);
      context.setAttribute(JettyWebSocket.WEBSOCKET_SERVER_FACTORY, wssf);
      context.addManaged(wssf);

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

  private void isNotInUse(List<String> protocols, String protocol, Consumer<String> consumer) {
    if (!protocols.contains(protocol)) {
      consumer.accept(protocol);
    }
  }

  @Nonnull @Override public synchronized io.jooby.Server stop() {
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
