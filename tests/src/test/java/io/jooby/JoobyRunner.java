package io.jooby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class JoobyRunner {

  static {
    System.setProperty("io.netty.leakDetection.level", "PARANOID");
    System.setProperty("io.netty.leakDetection.targetRecords", "20");
    System.setProperty("jooby.useShutdownHook", "false");
  }

  private final Supplier<Jooby> provider;

  private final List<ExecutionMode> modes = new ArrayList<>();

  private final String testName;

  private boolean followRedirects = true;

  public JoobyRunner(Consumer<Jooby> provider) {
    this.provider = () -> {
      Jooby app = new Jooby();
      provider.accept(app);
      return app;
    };
    this.testName = testName(new Exception());
  }

  public JoobyRunner(Supplier<Jooby> provider) {
    this.provider = provider;
    this.testName = testName(new Exception());
  }

  private String testName(Exception x) {
    StackTraceElement caller = Stream.of(x.getStackTrace())
        .skip(1)
        .findFirst()
        .get();
    String className = caller.getClassName();
    int i = className.lastIndexOf('.');
    if (i > 0) {
      className = className.substring(i + 1);
    }
    return className + "#" + caller.getMethodName();
  }

  public JoobyRunner mode(ExecutionMode... mode) {
    modes.addAll(Arrays.asList(mode));
    return this;
  }

  public JoobyRunner dontFollowRedirects() {
    followRedirects = false;
    return this;
  }

  public void ready(SneakyThrows.Consumer<WebClient> onReady) {
    ready(onReady, new Supplier[0]);
  }

  public void ready(SneakyThrows.Consumer2<WebClient, Server> onReady) {
    ready(onReady, new Supplier[0]);
  }

  public void ready(SneakyThrows.Consumer<WebClient> onReady, Supplier<Server>... servers) {
    ready((client, server) -> onReady.accept(client), servers);
  }

  public void ready(SneakyThrows.Consumer2<WebClient, Server> onReady,
      Supplier<Server>... servers) {
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
          .sorted(Comparator.comparing(Class::getSimpleName))
          .forEach(server -> serverList.add(SneakyThrows.throwingSupplier(server::newInstance)));
    } else {
      serverList.addAll(Arrays.asList(servers));
    }
    for (ExecutionMode mode : modes) {
      for (Supplier<Server> serverFactory : serverList) {
        Server server = serverFactory.get();
        try {
          System.setProperty(Jooby.APP_NAME, testName);
          Jooby app = this.provider.get().setExecutionMode(mode);
          ServerOptions serverOptions = app.getServerOptions();
          if (serverOptions != null) {
            server.setOptions(serverOptions);
          }
          ServerOptions options = server.getOptions();
          options.setPort(Integer.parseInt(System.getenv().getOrDefault("BUILD_PORT", "9999")));
          server.start(app);

          onReady.accept(new WebClient(options.getPort(), followRedirects), server);
        } catch (Throwable x) {
          x.printStackTrace();
          throw SneakyThrows.propagate(x);
        } finally {
          server.stop();
        }
      }
    }
  }
}

