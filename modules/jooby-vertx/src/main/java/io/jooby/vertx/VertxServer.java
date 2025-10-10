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
import io.jooby.internal.vertx.VertxRegistry;
import io.jooby.netty.NettyEventLoopGroup;
import io.jooby.netty.NettyServer;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Just a {@link NettyServer} server with a shared event loop group from <a
 * href="https://vertx.io/">Vertx</a>.
 *
 * <p>The following services are accessible from application registry:
 *
 * <ul>
 *   <li>{@link Vertx}
 *   <li>{@link io.vertx.core.eventbus.EventBus}
 *   <li>{@link io.vertx.core.file.FileSystem}
 * </ul>
 *
 * @author edgar
 * @since 4.0.8
 */
public class VertxServer extends NettyServer {
  private Vertx vertx;

  /**
   * Creates a new vertx server.
   *
   * @param vertx Use the provided vertx instance.
   */
  public VertxServer(@NonNull Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Creates a new vertx server.
   *
   * @param options Use the provided vertx options.
   */
  public VertxServer(@NonNull VertxOptions options) {
    this(Vertx.vertx(options));
  }

  /**
   * Creates a new vertx server with prefer native transport on and event loop size matching the
   * number of ioThreads from server options.
   */
  public VertxServer() {}

  @Override
  public Server init(@NonNull Jooby application) {
    if (this.vertx == null) {
      var nThreads = getOptions().getIoThreads();
      var options =
          new VertxOptions().setPreferNativeTransport(true).setEventLoopPoolSize(nThreads);
      this.vertx = Vertx.vertx(options);
    }

    VertxRegistry.init(application.getServices(), vertx);

    return super.init(application);
  }

  @Override
  public String getName() {
    return "vertx";
  }

  @Nullable @Override
  protected NettyEventLoopGroup createEventLoopGroup() {
    return new VertxEventLoopGroup(vertx);
  }

  @Override
  public synchronized Server stop() {
    super.stop();
    if (vertx != null) {
      vertx.close().await();
    }
    return this;
  }
}
