package io.jooby.jetty;

import io.jooby.App;
import io.jooby.Functions;
import io.jooby.Mode;
import io.jooby.Router;
import io.jooby.internal.jetty.JettyHandler;
import io.jooby.internal.jetty.JettyMultiHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.jooby.funzy.Throwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Jetty implements io.jooby.Server {

  private int port = 8080;

  private Server server;

  private List<App> applications = new ArrayList<>();

  @Override public io.jooby.Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public io.jooby.Server deploy(App application) {
    applications.add(application);
    return this;
  }

  @Nonnull @Override public io.jooby.Server start() {
    QueuedThreadPool executor = new QueuedThreadPool(64);
    executor.setName("jetty");

    this.server = new Server(executor);
    server.setStopAtShutdown(false);

    HttpConfiguration httpConf = new HttpConfiguration();
    httpConf.setOutputBufferSize(_16KB);
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

    JettyHandler handler = applications.size() == 1 ?
        new JettyHandler(applications.get(0)) :
        new JettyMultiHandler(null, applications);

    server.setHandler(handler);

    try {
      server.start();
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }

    applications.forEach(app -> {
      app.worker(Optional.ofNullable(app.worker()).orElse(executor));
      app.start(this);
    });

    return this;
  }

  @Override public io.jooby.Server stop() {
    try (Functions.Closer closer = Functions.closer()) {
      applications.forEach(app -> closer.register(app::stop));
      closer.register(server::stop);
    }
    return this;
  }
}
