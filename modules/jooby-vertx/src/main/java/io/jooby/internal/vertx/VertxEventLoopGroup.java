/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.netty.NettyEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;

public record VertxEventLoopGroup(Vertx vertx) implements NettyEventLoopGroup {

  @Override
  public @NonNull EventLoopGroup getParent() {
    return ((VertxInternal) vertx).acceptorEventLoopGroup();
  }

  @Override
  public @NonNull EventLoopGroup getChild() {
    return ((VertxInternal) vertx).nettyEventLoopGroup();
  }

  @Override
  public void shutdown() {
    // NOOP
  }
}
