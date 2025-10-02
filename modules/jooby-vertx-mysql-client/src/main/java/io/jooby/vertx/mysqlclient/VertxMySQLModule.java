/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.mysqlclient;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.vertx.sqlclient.VertxSqlClientModule;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.*;

public class VertxMySQLModule<C extends SqlClient> extends VertxSqlClientModule<C> {

  private final Supplier<ClientBuilder<C>> builder;

  protected VertxMySQLModule(@NonNull String name, @NonNull Supplier<ClientBuilder<C>> builder) {
    super(name);
    this.builder = builder;
  }

  @Override
  protected SqlConnectOptions fromMap(JsonObject config) {
    return new MySQLConnectOptions(config);
  }

  @Override
  protected SqlConnectOptions fromUri(String uri) {
    return MySQLConnectOptions.fromUri(uri);
  }

  @Override
  protected ClientBuilder<C> newBuilder() {
    return builder.get();
  }

  public static VertxMySQLModule<Pool> pool() {
    return pool(new PoolOptions());
  }

  public static VertxMySQLModule<Pool> pool(PoolOptions options) {
    return pool("db", options);
  }

  public static VertxMySQLModule<Pool> pool(String name) {
    return pool(name, new PoolOptions());
  }

  public static VertxMySQLModule<Pool> pool(String name, PoolOptions options) {
    return new VertxMySQLModule<>(name, () -> MySQLBuilder.pool().with(options));
  }

  public static VertxMySQLModule<SqlClient> client() {
    return client("db");
  }

  public static VertxMySQLModule<SqlClient> client(String name) {
    return new VertxMySQLModule<>(name, MySQLBuilder::client);
  }
}
