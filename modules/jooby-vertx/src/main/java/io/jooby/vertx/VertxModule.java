/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import java.util.function.Function;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.vertx.VertxRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

/**
 * A <a href="https://vertx.io/">Vertx</a> module.
 *
 * <p>The following services are accessible from application registry:
 *
 * <ul>
 *   <li>{@link Vertx}
 *   <li>{@link io.vertx.core.eventbus.EventBus}
 *   <li>{@link io.vertx.core.file.FileSystem}
 * </ul>
 *
 * Options might be provided from application configuration file:
 *
 * <pre>{@code
 * vertx.eventLoopPoolSize=5
 * vertx.workerPoolSize = 20
 * vertx.blockedThreadCheckInterval=500
 * vertx.maxEventLoopExecuteTime=2000
 * }</pre>
 *
 * @author edgar
 * @since 4.0.8
 */
public class VertxModule implements Extension {
  private VertxOptions options;
  private final Function<VertxOptions, Future<Vertx>> vertxFactory;

  /**
   * Creates a new vertx module. Options might be provided from application configuration using the
   * <code>vertx</code> prefix.
   */
  public VertxModule() {
    this(options -> Future.succeededFuture(Vertx.vertx(options)));
  }

  /**
   * Creates a new vertx module.
   *
   * @param vertx Vertx instance.
   */
  public VertxModule(Vertx vertx) {
    this(options -> Future.succeededFuture(vertx));
  }

  /**
   * Creates a new vertx module.
   *
   * @param options Vertx options.
   */
  public VertxModule(VertxOptions options) {
    this(ops -> Future.succeededFuture(Vertx.vertx(ops)));
    this.options = options;
  }

  /**
   * Creates a new vertx module.
   *
   * @param vertx Vertx provider.
   */
  public VertxModule(Function<VertxOptions, Future<Vertx>> vertx) {
    this.vertxFactory = vertx;
  }

  /**
   * Creates a new vertx module.
   *
   * @param vertx Vertx provider.
   */
  public VertxModule(Supplier<Future<Vertx>> vertx) {
    this(options -> vertx.get());
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    // Ensure single instance
    if (application.getServices().getOrNull(Vertx.class) != null) {
      throw new IllegalStateException("Vertx already exists.");
    }
    var config = application.getConfig();
    if (options == null) {
      if (config.hasPath("vertx")) {
        options = new VertxOptions(new JsonObject(config.getObject("vertx").unwrapped()));
      } else {
        options = new VertxOptions();
      }
    }
    var vertxFuture = vertxFactory.apply(options);
    var vertx = vertxFuture.await();

    VertxRegistry.init(application.getServices(), vertx);

    application.onStop(() -> vertx.close().await());
  }
}
