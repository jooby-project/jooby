/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.pgclient;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.vertx.sqlclient.VertxSqlClientModule;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.*;

public class VertxPgModule extends VertxSqlClientModule {

  private final Supplier<ClientBuilder<? extends SqlClient>> builder;

  public VertxPgModule(
      @NonNull String name, @NonNull Supplier<ClientBuilder<? extends SqlClient>> builder) {
    super(name);
    this.builder = builder;
  }

  public VertxPgModule(@NonNull Supplier<ClientBuilder<? extends SqlClient>> builder) {
    this("db", builder);
  }

  @Override
  protected SqlConnectOptions fromMap(JsonObject config) {
    return new PgConnectOptions(config);
  }

  @Override
  protected SqlConnectOptions fromUri(String uri) {
    return PgConnectOptions.fromUri(uri);
  }

  @Override
  protected ClientBuilder<? extends SqlClient> newBuilder() {
    return builder.get();
  }
}
