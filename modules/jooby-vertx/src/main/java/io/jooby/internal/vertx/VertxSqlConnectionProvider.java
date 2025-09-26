/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import io.vertx.core.impl.VertxThread;
import io.vertx.sqlclient.SqlConnection;
import jakarta.inject.Provider;

public class VertxSqlConnectionProvider<T extends SqlConnection> implements Provider<T> {
  private static final ThreadLocal<Object> connectionHolder = new ThreadLocal<>();

  @SuppressWarnings("unchecked")
  @Override
  public T get() {
    var thread = Thread.currentThread();
    if (!(thread instanceof VertxThread)) {
      throw new IllegalStateException("Current thread is not a vertx thread");
    }
    return (T) connectionHolder.get();
  }

  public static <T extends SqlConnection> void set(T connection) {
    connectionHolder.set(connection);
  }
}
