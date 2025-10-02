/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import io.vertx.sqlclient.SqlClient;
import jakarta.inject.Provider;

public record VertxSqlClientProvider<T extends SqlClient>(String name)
    implements Provider<SqlClient> {

  @SuppressWarnings("unchecked")
  @Override
  public T get() {
    return (T) new VertxThreadLocalSqlConnection(name);
  }
}
