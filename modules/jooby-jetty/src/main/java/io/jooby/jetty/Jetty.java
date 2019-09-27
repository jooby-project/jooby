/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jetty;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.internal.jetty.JettyHandler;
import io.jooby.internal.jetty.JettyWebSocket;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

import javax.annotation.Nonnull;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

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

      HttpConfiguration httpConf = new HttpConfiguration();
      httpConf.setOutputBufferSize(options.getBufferSize());
      httpConf.setOutputAggregationSize(options.getBufferSize());
      httpConf.setSendXPoweredBy(false);
      httpConf.setSendDateHeader(options.getDefaultHeaders());
      httpConf.setSendServerVersion(false);
      httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
      ServerConnector connector = new ServerConnector(server);
      connector.addConnectionFactory(new HttpConnectionFactory(httpConf));
      connector.setPort(options.getPort());
      connector.setHost("0.0.0.0");

      server.addConnector(connector);

      ContextHandler context = new ContextHandler();

      AbstractHandler handler = new JettyHandler(applications.get(0), options.getBufferSize(),
          options.getMaxRequestSize(), options.getDefaultHeaders());

      if (options.getGzip()) {
        GzipHandler gzipHandler = new GzipHandler();
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
      WebSocketServerFactory wssf = new WebSocketServerFactory(context.getServletContext(), policy);
      context.setAttribute(JettyWebSocket.WEBSOCKET_SERVER_FACTORY, wssf);
      context.addManaged(wssf);

      server.setHandler(context);

      server.start();

      fireReady(applications);
    } catch (Exception x) {
      if (x.getCause() instanceof BindException) {
        x = new BindException("Address already in use: " + options.getPort());
      }
      throw SneakyThrows.propagate(x);
    }

    return this;
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
