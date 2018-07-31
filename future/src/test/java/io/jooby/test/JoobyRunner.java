package io.jooby.test;

import io.jooby.App;
import io.jooby.Server;
import io.jooby.netty.Netty;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class JoobyRunner {

  private final Supplier<App> provider;

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

  public void ready(Consumer<WebClient> onReady, Server... servers) {
    for (Server server : servers) {
      App app = null;
      try {
        app = this.provider.get();
        app.use(server);
        app.start(false);
        onReady.accept(new WebClient(8080));
      } finally {
        if (app != null) {
          app.stop();
        }
      }
    }
  }

  public void ready(Consumer<WebClient> onReady) {
    ready(onReady, new Netty());
  }

}
