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
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.*;

public class VertxPgModule<C extends SqlClient> extends VertxSqlClientModule<C> {

  private final Supplier<ClientBuilder<C>> builder;

  protected VertxPgModule(@NonNull String name, @NonNull Supplier<ClientBuilder<C>> builder) {
    super(name);
    this.builder = builder;
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
  protected ClientBuilder<C> newBuilder() {
    return builder.get();
  }

  public static VertxPgModule<Pool> pool() {
    return pool(new PoolOptions());
  }

  public static VertxPgModule<Pool> pool(PoolOptions options) {
    return pool("db", options);
  }

  public static VertxPgModule<Pool> pool(String name) {
    return pool(name, new PoolOptions());
  }

  public static VertxPgModule<Pool> pool(String name, PoolOptions options) {
    return new VertxPgModule<>(name, () -> PgBuilder.pool().with(options));
  }

  public static VertxPgModule<SqlClient> client() {
    return client("db");
  }

  public static VertxPgModule<SqlClient> client(String name) {
    return new VertxPgModule<>(name, PgBuilder::client);
  }
}
