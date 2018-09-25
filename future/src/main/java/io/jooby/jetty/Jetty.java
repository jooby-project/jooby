package io.jooby.jetty;

import io.jooby.Context;
import io.jooby.Mode;
import io.jooby.Router;
import io.jooby.internal.jetty.JettyHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
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
    QueuedThreadPool executor = new QueuedThreadPool(64);
    executor.setName("jetty");

    this.server = new Server(executor);
    server.setStopAtShutdown(false);

    HttpConfiguration httpConf = new HttpConfiguration();
    httpConf.setOutputBufferSize(Context._16KB);
    httpConf.setSendXPoweredBy(false);
    httpConf.setSendDateHeader(false);
    httpConf.setSendServerVersion(false);
    httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
    int acceptors = 1;
    int selectors = Runtime.getRuntime().availableProcessors();
    Scheduler scheduler = new ScheduledExecutorScheduler("jetty-scheduler", false);
    ServerConnector connector = new ServerConnector(server, executor, scheduler, null,
        acceptors, selectors, new HttpConnectionFactory(httpConf));
    connector.setPort(port);
    connector.setHost("0.0.0.0");

    server.addConnector(connector);

    server.setHandler(new JettyHandler(router, tmpdir));

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
