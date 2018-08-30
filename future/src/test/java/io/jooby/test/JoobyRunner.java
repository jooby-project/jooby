package io.jooby.test;

import io.jooby.App;
import io.jooby.Mode;
import io.jooby.Server;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JoobyRunner {

  private final Supplier<App> provider;

  private final List<Mode> modes = new ArrayList<>();

  public JoobyRunner(Consumer<App> configurer) {
    this(() -> {
      App app = new App();
      configurer.accept(app);
      return app;
    });
  }

  public JoobyRunner(Supplier<App> provider) {
    this.provider = provider;
  }

  public JoobyRunner mode(Mode... mode) {
    modes.addAll(Arrays.asList(mode));
    return this;
  }

  public void ready(Consumer<WebClient> onReady, Server... servers) {
    if (modes.size() == 0) {
      modes.add(Mode.WORKER);
    }
    List<Server> serverList = new ArrayList<>();
    if (servers.length == 0) {
      serverList.add(new Netty());
      serverList.add(new Utow());
      serverList.add(new Jetty());
    } else {
      serverList.addAll(Arrays.asList(servers));
    }
    for (Mode mode : modes) {
      for (Server server : serverList) {
        App app = null;
        try {
          app = this.provider.get();
          app.mode(mode);
          app.port(9999);
          app.use(server);
          app.start(false);
          onReady.accept(new WebClient(9999));
        } finally {
          if (app != null) {
            app.stop();
          }
        }
      }
    }
  }
}
