package io.jooby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class JoobyRunner {

  static {
    System.setProperty("io.netty.leakDetection.level", "PARANOID");
  }

  private final Supplier<Jooby> provider;

  private final List<ExecutionMode> modes = new ArrayList<>();

  public JoobyRunner(Consumer<Jooby> provider) {
    this.provider = () -> {
      Jooby app = new Jooby();
      provider.accept(app);
      return app;
    };
  }

  public JoobyRunner(Supplier<Jooby> provider) {
    this.provider = provider;
  }

  public JoobyRunner mode(ExecutionMode... mode) {
    modes.addAll(Arrays.asList(mode));
    return this;
  }

  public void ready(Throwing.Consumer<WebClient> onReady) {
    ready(onReady, new Supplier[0]);
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
          Jooby app = this.provider.get().setExecutionMode(mode);
          ServerOptions serverOptions = app.getServerOptions();
          if (serverOptions != null) {
            server.setOptions(serverOptions);
          }
          server.getOptions().setPort(9999);
          server.start(app);

          onReady.accept(new WebClient(9999));
        } catch (Throwable x) {
          x.printStackTrace();
          throw Throwing.sneakyThrow(x);
        } finally {
          server.stop();
        }
      }
    }
  }
}

