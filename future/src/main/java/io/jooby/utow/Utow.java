package io.jooby.utow;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.internal.utow.UtowBlockingHandler;
import io.jooby.internal.utow.UtowHandler;
import io.jooby.internal.utow.UtowMultiHandler;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private List<App> applications = new ArrayList<>();

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Nonnull @Override public Server deploy(App application) {
    applications.add(application);
    return this;
  }

  @Override public Server start() {
    HttpHandler uhandler;
    if (applications.size() == 1) {
      uhandler = newHandler(applications.get(0));
    } else {
      Map<App, UtowHandler> handlers = new LinkedHashMap<>(applications.size());
      applications.forEach(app -> handlers.put(app, newHandler(app)));
      uhandler = new UtowMultiHandler(handlers);
    }

    server = Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setBufferSize(_16KB)
        // HTTP/1.1 is keep-alive by default
        .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
        .setServerOption(UndertowOptions.ALWAYS_SET_DATE, false)
        .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
        .setServerOption(UndertowOptions.DECODE_URL, false)
        .setHandler(uhandler)
        .build();

    server.start();

    for (Router router : applications) {
      router.start();
      Logger log = LoggerFactory.getLogger(router.getClass());
      log.info("{}\n\n{}\n\nhttp://localhost:{}{}\n", router.getClass().getSimpleName(), router,
          port, router.basePath());
    }

    return this;
  }

  private UtowHandler newHandler(App app) {
    return app.mode() == Mode.WORKER ? new UtowBlockingHandler(app) : new UtowHandler(app);
  }

  @Override public Server stop() {
    applications.clear();
    server.stop();
    return this;
  }

}
