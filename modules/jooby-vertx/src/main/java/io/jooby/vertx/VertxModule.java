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
import io.vertx.core.json.JsonObject;

public class VertxModule implements Extension {
  private Vertx vertx;

  public VertxModule(Vertx vertx) {
    this.vertx = vertx;
  }

  public VertxModule(VertxOptions options) {
    this.vertx = Vertx.vertx(options);
  }

  public VertxModule() {}

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    if (vertx == null) {
      var config = application.getConfig();
      VertxOptions options;
      if (config.hasPath("vertx")) {
        options = new VertxOptions(new JsonObject(config.getObject("vertx").unwrapped()));
      } else {
        options = new VertxOptions();
      }
      vertx = Vertx.vertx(options);
    }
    var registry = application.getServices();
    registry.put(Vertx.class, vertx);
    application.onStop(() -> vertx.close().await());
  }
}
