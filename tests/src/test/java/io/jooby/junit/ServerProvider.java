/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.junit;

import static java.util.stream.StreamSupport.stream;

import java.util.ServiceLoader;

import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.netty.NettyServer;
import io.jooby.undertow.UndertowServer;

public class ServerProvider {
  private Class serverClass;

  public ServerProvider(Class serverClass) {
    this.serverClass = serverClass;
  }

  public boolean matchesEventLoopThread(String threadName) {
    if (serverClass == JettyServer.class) {
      return threadName.startsWith("worker-");
    }
    if (serverClass == NettyServer.class) {
      return threadName.startsWith("eventloop");
    }
    if (serverClass == UndertowServer.class) {
      return threadName.startsWith("worker I/O");
    }
    return false;
  }

  public String getName() {
    return serverClass.getSimpleName().replace("Server", "");
  }

  public Server get(ServerOptions options) {
    var server =
        stream(ServiceLoader.load(Server.class).spliterator(), false)
            .filter(s -> serverClass.isInstance(s))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverClass));
    if (options != null) {
      server.setOptions(options);
    }
    return server;
  }
}
