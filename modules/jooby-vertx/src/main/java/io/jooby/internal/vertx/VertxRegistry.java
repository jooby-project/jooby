/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import io.jooby.ServiceRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;

public class VertxRegistry {

  public static void init(ServiceRegistry registry, Vertx vertx) {
    registry.put(Vertx.class, vertx);
    registry.put(EventBus.class, vertx.eventBus());
    registry.put(FileSystem.class, vertx.fileSystem());
  }
}
