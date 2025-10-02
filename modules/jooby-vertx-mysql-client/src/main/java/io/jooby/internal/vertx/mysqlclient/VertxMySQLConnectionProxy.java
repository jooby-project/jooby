/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.mysqlclient;

import io.jooby.internal.vertx.sqlclient.VertxThreadLocalSqlConnection;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mysqlclient.MySQLAuthOptions;
import io.vertx.mysqlclient.MySQLConnection;
import io.vertx.mysqlclient.MySQLSetOption;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.spi.DatabaseMetadata;

public record VertxMySQLConnectionProxy(String name) implements MySQLConnection {

  private MySQLConnection get() {
    return (MySQLConnection) VertxThreadLocalSqlConnection.get(name);
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

  @Override
  public MySQLConnection exceptionHandler(Handler<Throwable> handler) {
    return get().exceptionHandler(handler);
  }

  @Override
  public MySQLConnection closeHandler(Handler<Void> handler) {
    return get().closeHandler(handler);
  }

  @Override
  public Future<Void> ping() {
    return get().ping();
  }

  @Override
  public Future<Void> specifySchema(String s) {
    return get().specifySchema(s);
  }

  @Override
  public Future<String> getInternalStatistics() {
    return get().getInternalStatistics();
  }

  @Override
  public Future<Void> setOption(MySQLSetOption mySQLSetOption) {
    return get().setOption(mySQLSetOption);
  }

  @Override
  public Future<Void> resetConnection() {
    return get().resetConnection();
  }

  @Override
  public Future<Void> debug() {
    return get().debug();
  }

  @Override
  public Future<Void> changeUser(MySQLAuthOptions mySQLAuthOptions) {
    return get().changeUser(mySQLAuthOptions);
  }
}
