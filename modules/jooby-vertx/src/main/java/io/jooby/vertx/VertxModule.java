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
import io.jooby.internal.vertx.VertxEncoder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;

public class VertxModule implements Extension {
  private VertxOptions options;
  private final Function<VertxOptions, Future<Vertx>> vertxFactory;

  public VertxModule() {
    this(options -> Future.succeededFuture(Vertx.vertx(options)));
  }

  public VertxModule(Vertx vertx) {
    this(options -> Future.succeededFuture(vertx));
  }

  public VertxModule(VertxOptions options) {
    this(ops -> Future.succeededFuture(Vertx.vertx(ops)));
    this.options = options;
  }

  public VertxModule(Function<VertxOptions, Future<Vertx>> vertx) {
    this.vertxFactory = vertx;
  }

  public VertxModule(Supplier<Future<Vertx>> vertx) {
    this(options -> vertx.get());
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
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
    var registry = application.getServices();
    registry.put(Vertx.class, vertx);
    registry.put(EventBus.class, vertx.eventBus());
    registry.put(FileSystem.class, vertx.fileSystem());

    application.encoder(new VertxEncoder());

    application.onStop(() -> vertx.close().await());
  }
}
