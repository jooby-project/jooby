/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxThread;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.SqlClientInternal;
import io.vertx.sqlclient.spi.DatabaseMetadata;
import io.vertx.sqlclient.spi.Driver;

public record VertxThreadLocalSqlConnection(String name)
    implements SqlConnection, SqlClientInternal {
  private static final ThreadLocal<Map<String, SqlConnection>> connectionHolder =
      ThreadLocal.withInitial(HashMap::new);

  public static SqlConnection get(String name) {
    var thread = Thread.currentThread();
    if (!(thread instanceof VertxThread)) {
      throw new IllegalStateException("Current thread is not a vertx thread");
    }
    return connectionHolder.get().get(name);
  }

  public static <T extends SqlConnection> void set(String name, T connection) {
    connectionHolder.get().put(name, connection);
  }

  private SqlConnection get() {
    var thread = Thread.currentThread();
    if (!(thread instanceof VertxThread)) {
      throw new IllegalStateException("Current thread is not a vertx thread");
    }
    return get(name);
  }

  @Override
  public Driver driver() {
    return ((SqlClientInternal) get()).driver();
  }

  @Override
  public void group(Handler<SqlClient> block) {
    ((SqlClientInternal) get()).group(block);
  }

  @Override
  public Query<RowSet<Row>> query(String sql) {
    return get().query(sql);
  }

  @Override
  public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
    return get().preparedQuery(sql);
  }

  @Override
  public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
    return get().preparedQuery(sql, options);
  }

  @Override
  public Future<Void> close() {
    return get().close();
  }

  @Override
  @NonNull public String toString() {
    return Thread.currentThread().getName() + ":" + name;
  }

  @Override
  public Future<PreparedStatement> prepare(String sql) {
    return get().prepare(sql);
  }

  @Override
  public Future<PreparedStatement> prepare(String sql, PrepareOptions options) {
    return get().prepare(sql, options);
  }

  @Override
  public SqlConnection exceptionHandler(Handler<Throwable> handler) {
    return get().exceptionHandler(handler);
  }

  @Override
  public SqlConnection closeHandler(Handler<Void> handler) {
    return get().closeHandler(handler);
  }

  @Override
  public Future<Transaction> begin() {
    return get().begin();
  }

  @Override
  public Transaction transaction() {
    return get().transaction();
  }

  @Override
  public boolean isSSL() {
    return get().isSSL();
  }

  @Override
  public DatabaseMetadata databaseMetadata() {
    return get().databaseMetadata();
  }
}
