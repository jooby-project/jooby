/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class VertxModule implements Extension {
  private Vertx vertx;

  public VertxModule(@NonNull Vertx vertx) {
    this.vertx = vertx;
  }

  public VertxModule(@NonNull VertxOptions options) {
    this(Vertx.vertx(options));
  }

  public VertxModule() {}

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    if (this.vertx == null) {
      var options =
          new VertxOptions()
              .setPreferNativeTransport(true)
              .setEventLoopPoolSize(application.getServerOptions().getIoThreads());
      this.vertx = Vertx.vertx(options);
    }
    var registry = application.getServices();
    registry.put(Vertx.class, vertx);
  }
}
