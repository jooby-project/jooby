package io.jooby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class JoobyRunner {

  private final Supplier<Jooby> provider;

  private final List<ExecutionMode> modes = new ArrayList<>();

  private Consumer<Server> serverConfigurer;

  public JoobyRunner(Consumer<Jooby> provider) {
    this.provider = () -> {
      try {
        Jooby.setEnv(Env.defaultEnvironment("test"));
        Jooby app = new Jooby();
        provider.accept(app);
        return app;
      } finally {
        Jooby.setEnv(null);
      }
    };
  }

  public JoobyRunner(Supplier<Jooby> provider) {
    this.provider = () -> {
      try {
        Jooby.setEnv(Env.defaultEnvironment("test"));
        return provider.get();
      } finally {
        Jooby.setEnv(null);
      }
    };
  }

  public JoobyRunner mode(ExecutionMode... mode) {
    modes.addAll(Arrays.asList(mode));
    return this;
  }

  public JoobyRunner configureServer(Consumer<Server> configurer) {
    this.serverConfigurer = configurer;
    return this;
  }

  public void ready(Throwing.Consumer<WebClient> onReady, Supplier<Server>... servers) {
    if (modes.size() == 0) {
      modes.add(ExecutionMode.DEFAULT);
    }
    List<Supplier<Server>> serverList = new ArrayList<>();
    if (servers.length == 0) {
      stream(
          spliteratorUnknownSize(
              ServiceLoader.load(Server.class).iterator(),
              Spliterator.ORDERED),
          false)
          .map(Server::getClass)
          .forEach(server -> serverList.add(Throwing.throwingSupplier(server::newInstance)));
    } else {
      serverList.addAll(Arrays.asList(servers));
    }
    for (ExecutionMode mode : modes) {
      for (Supplier<Server> serverFactory : serverList) {
        Server server = serverFactory.get();
        try {
          if (serverConfigurer != null) {
            serverConfigurer.accept(server);
          }
          Jooby app = this.provider.get().mode(mode);
          server.port(9999)
              .start(app);

          onReady.accept(new WebClient(9999));
        } finally {
          server.stop();
        }
      }
    }
  }
}

