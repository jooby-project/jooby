/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.pgclient;

import io.jooby.internal.vertx.sqlclient.VertxThreadLocalSqlConnection;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgNotice;
import io.vertx.pgclient.PgNotification;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.spi.DatabaseMetadata;

public record VertxPgConnectionProxy(String name) implements PgConnection {

  private PgConnection get() {
    return (PgConnection) VertxThreadLocalSqlConnection.get(name);
  }

  @Override
  public PgConnection notificationHandler(Handler<PgNotification> handler) {
    return get().notificationHandler(handler);
  }

  @Override
  public PgConnection noticeHandler(Handler<PgNotice> handler) {
    return get().noticeHandler(handler);
  }

  @Override
  public Future<Void> cancelRequest() {
    return get().cancelRequest();
  }

  @Override
  public int processId() {
    return get().processId();
  }

  @Override
  public int secretKey() {
    return get().secretKey();
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
  public PgConnection exceptionHandler(Handler<Throwable> handler) {
    return get().exceptionHandler(handler);
  }

  @Override
  public PgConnection closeHandler(Handler<Void> handler) {
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
}
