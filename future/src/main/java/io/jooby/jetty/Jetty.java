package io.jooby.jetty;

import io.jooby.Context;
import io.jooby.Mode;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.internal.jetty.JettyContext;
import io.jooby.internal.jetty.JettyHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Jetty implements io.jooby.Server {

  private int port = 8080;

  private Server server;

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));

  @Override public io.jooby.Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public io.jooby.Server mode(Mode mode) {
    // NOOP: Jetty always run in a worker thread.
    return this;
  }

  @Nonnull @Override public io.jooby.Server tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public io.jooby.Server start(Router router) {
    QueuedThreadPool pool = new QueuedThreadPool(64);

    this.server = new Server(pool);
    server.setStopAtShutdown(false);

    HttpConfiguration httpConf = new HttpConfiguration();
    httpConf.setOutputBufferSize(Context._16KB);
    httpConf.setSendXPoweredBy(false);
    httpConf.setSendDateHeader(false);
    httpConf.setSendServerVersion(false);
    httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
    int acceptors = 1;
    int selectors = Runtime.getRuntime().availableProcessors();
    ServerConnector connector = new ServerConnector(server, acceptors, selectors,
        new HttpConnectionFactory(httpConf));
    connector.setPort(port);
    connector.setHost("0.0.0.0");

    server.addConnector(connector);

    server.setHandler(new JettyHandler(router, server.getThreadPool(), tmpdir));

    try {
      server.start();
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public io.jooby.Server stop() {
    try {
      server.stop();
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
