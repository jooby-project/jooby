package io.jooby.junit;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import io.jooby.WebClient;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ServerTestRunner {

  private final String testName;

  private final Server server;

  private final ExecutionMode executionMode;

  private Supplier<Jooby> provider;

  private boolean followRedirects = true;

  public ServerTestRunner(String testName, Server server, ExecutionMode executionMode) {
    this.testName = testName;
    this.server = server;
    this.executionMode = executionMode;
  }

  public ServerTestRunner define(Consumer<Jooby> consumer) {
    define(() -> {
      Jooby app = new Jooby();
      consumer.accept(app);
      return app;
    });
    return this;
  }

  public ServerTestRunner define(Supplier<Jooby> provider) {
    this.provider = provider;
    return this;
  }

  public void ready(SneakyThrows.Consumer<WebClient> onReady) {
    ready((http, https) -> onReady.accept(http));
  }

  public void ready(SneakyThrows.Consumer2<WebClient, WebClient> onReady) {
    try {
      System.setProperty("___app_name__", testName);
      Jooby app = provider.get();
      Optional.ofNullable(executionMode).ifPresent(app::setExecutionMode);
      ServerOptions serverOptions = app.getServerOptions();
      if (serverOptions != null) {
        server.setOptions(serverOptions);
      }
      ServerOptions options = server.getOptions();
      options.setPort(Integer.parseInt(System.getenv().getOrDefault("BUILD_PORT", "9999")));
      WebClient https;
      if (options.isSSLEnabled()) {
        options.setSecurePort(
            Integer.parseInt(System.getenv().getOrDefault("BUILD_SECURE_PORT", "9443")));
        https = new WebClient("https", options.getSecurePort(), followRedirects);
      } else {
        https = null;
      }
      server.start(app);
      try (WebClient http = new WebClient("http", options.getPort(), followRedirects)) {
        onReady.accept(http, https);
      } finally {
        if (https != null) {
          https.close();
        }
      }
    } catch (AssertionError x) {
      throw SneakyThrows.propagate(serverInfo(x));
    } finally {
      server.stop();
    }
  }

  public String getServer() {
    return server.getClass().getSimpleName();
  }

  public ServerTestRunner dontFollowRedirects() {
    followRedirects = false;
    return this;
  }

  public boolean matchesEventLoopThread(String threadName) {
    if (server instanceof Jetty) {
      return threadName.startsWith("worker-");
    }
    if (server instanceof Netty) {
      return threadName.startsWith("eventloop");
    }
    if (server instanceof Utow) {
      return threadName.startsWith("worker I/O");
    }
    return false;
  }

  @Override public String toString() {
    StringBuilder message = new StringBuilder();
    message.append(testName).append("(");
    message.append(getServer());
    if (executionMode != null) {
      message.append(", ").append(executionMode);
    }
    message.append(")");
    return message.toString();
  }

  private static String testName(Exception x) {
    StackTraceElement caller = Stream.of(x.getStackTrace())
        .skip(1)
        .findFirst()
        .get();
    String className = caller.getClassName();
    int i = className.lastIndexOf('.');
    if (i > 0) {
      className = className.substring(i + 1);
    }
    return className + "." + caller.getMethodName();
  }

  private AssertionError serverInfo(AssertionError x) {
    StringBuilder message = new StringBuilder();
    message.append(this);
    Optional.ofNullable(x.getMessage())
        .ifPresent(m -> message.append(": ").append(m));
    AssertionError error = new AssertionError(message.toString().trim());
    error.setStackTrace(x.getStackTrace());
    return error;
  }
}
