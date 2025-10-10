/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.junit;

import io.jooby.Server;
import io.jooby.ServerOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.netty.NettyServer;
import io.jooby.undertow.UndertowServer;
import io.jooby.vertx.VertxServer;

public record ServerProvider(Class<?> serverClass) {

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
    if (serverClass == VertxServer.class) {
      return threadName.startsWith("vert.x-");
    }
    return false;
  }

  public String getName() {
    return serverClass.getSimpleName().replace("Server", "");
  }

  public Server get(ServerOptions options) {
    Server server = createServer(serverClass);
    if (options != null) {
      server.setOptions(options);
    }
    return server;
  }

  private Server createServer(Class<?> serverClass) {
    if (serverClass == JettyServer.class) {
      return new JettyServer();
    }
    if (serverClass == NettyServer.class) {
      return new NettyServer();
    }
    if (serverClass == UndertowServer.class) {
      return new UndertowServer();
    }
    if (serverClass == VertxServer.class) {
      return new VertxServer();
    }
    throw new IllegalArgumentException("Server not found: " + serverClass);
  }
}
