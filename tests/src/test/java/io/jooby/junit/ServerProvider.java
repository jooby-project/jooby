package io.jooby.junit;

import io.jooby.Server;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

import java.util.ServiceLoader;
import java.util.function.Supplier;

import static java.util.stream.StreamSupport.stream;

public class ServerProvider implements Supplier<Server> {
  private Class serverClass;

  public ServerProvider(Class serverClass) {
    this.serverClass = serverClass;
  }

  public boolean matchesEventLoopThread(String threadName) {
    if (serverClass == Jetty.class) {
      return threadName.startsWith("worker-");
    }
    if (serverClass == Netty.class) {
      return threadName.startsWith("eventloop");
    }
    if (serverClass == Utow.class) {
      return threadName.startsWith("worker I/O");
    }
    return false;
  }

  public String getName() {
    return serverClass.getSimpleName();
  }

  @Override public Server get() {
    return stream(ServiceLoader.load(Server.class).spliterator(), false)
        .filter(s -> serverClass.isInstance(s))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverClass));
  }
}
