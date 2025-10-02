/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.internal.vertx.VertxEventLoopGroup;
import io.jooby.netty.NettyEventLoopGroup;
import io.jooby.netty.NettyServer;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class VertxServer extends NettyServer {
  private Vertx vertx;

  public VertxServer(@NonNull Vertx vertx) {
    this.vertx = vertx;
  }

  public VertxServer(@NonNull VertxOptions options) {
    this(Vertx.vertx(options));
  }

  public VertxServer() {}

  @Override
  public @NonNull Server init(Jooby application) {
    if (this.vertx == null) {
      var nThreads = getOptions().getIoThreads();
      var options =
          new VertxOptions().setPreferNativeTransport(true).setEventLoopPoolSize(nThreads);
      this.vertx = Vertx.vertx(options);
    }
    application.getServices().put(Vertx.class, vertx);
    return super.init(application);
  }

  @Nullable @Override
  protected NettyEventLoopGroup createEventLoopGroup() {
    return new VertxEventLoopGroup(vertx);
  }

  @Override
  public synchronized @NonNull Server stop() {
    super.stop();
    if (vertx != null) {
      vertx.close().await();
    }
    return this;
  }
}
