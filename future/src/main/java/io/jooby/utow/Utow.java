package io.jooby.utow;

import io.jooby.App;
import io.jooby.Functions;
import io.jooby.Mode;
import io.jooby.Server;
import io.jooby.internal.utow.UtowBlockingHandler;
import io.jooby.internal.utow.UtowHandler;
import io.jooby.internal.utow.UtowMultiHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import org.xnio.Options;
import org.xnio.XnioWorker;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private List<App> applications = new ArrayList<>();

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public Server deploy(App application) {
    applications.add(application);
    return this;
  }

  @Override public Server start() {
    Map<App, UtowHandler> handlers = new LinkedHashMap<>(applications.size());
    applications.forEach(app -> handlers.put(app, newHandler(app)));

    HttpHandler handler = handlers.size() == 1
        ? handlers.values().iterator().next()
        : new UtowMultiHandler(handlers);

    server = Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setBufferSize(_16KB)
        // HTTP/1.1 is keep-alive by default, turn this option off
        .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
        .setServerOption(Options.BACKLOG, 10000)
        .setServerOption(Options.REUSE_ADDRESSES, true)
        .setServerOption(UndertowOptions.ALWAYS_SET_DATE, false)
        .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
        .setServerOption(UndertowOptions.DECODE_URL, false)
        .setHandler(handler)
        .build();

    server.start();

    applications.forEach(app -> {
      XnioWorker worker = server.getWorker();
      app.executor("io", worker.getIoThread());
      try {
        app.executor("worker");
      } catch (NoSuchElementException x) {
        app.executor("worker", worker);
      }
      java.util.concurrent.Executor executor = app.executor(Mode.IO.name().toLowerCase());
      handlers.get(app).executor(executor);

      app.start(this);
    });

    return this;
  }

  private UtowHandler newHandler(App app) {
    return app.mode() == Mode.WORKER ? new UtowBlockingHandler(app) : new UtowHandler(app);
  }

  @Override public Server stop() {
    try (Functions.Closer closer = Functions.closer()) {
      applications.forEach(app -> closer.register(app::stop));
      closer.register(server::stop);
    }
    return this;
  }

}
