/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import java.util.concurrent.ExecutorService;

import io.jooby.netty.NettyEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

public record VertxEventLoopGroup(Vertx vertx) implements NettyEventLoopGroup {

  @Override
  public EventLoopGroup acceptor() {
    return ((VertxInternal) vertx).acceptorEventLoopGroup();
  }

  @Override
  public EventLoopGroup eventLoop() {
    return ((VertxInternal) vertx).nettyEventLoopGroup();
  }

  @Override
  public ExecutorService worker() {
    return ((VertxInternal) vertx).workerPool().executor();
  }

  @Override
  public void shutdown() {
    // NOOP
  }
}
