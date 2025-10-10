/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.mysqlclient;

import java.util.List;
import java.util.Map;

import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.internal.vertx.mysqlclient.VertxMySQLConnectionProxy;
import io.jooby.internal.vertx.sqlclient.VertxSqlClientProvider;
import io.jooby.internal.vertx.sqlclient.VertxSqlConnectionVerticle;
import io.jooby.vertx.sqlclient.VertxSqlConnectionModule;
import io.vertx.core.Deployable;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLConnection;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.impl.SqlClientInternal;

/**
 * These modules exist as part of the performance tests required by <a
 * href="https://www.techempower.com/benchmarks">Techempower</a>.
 *
 * <p>Define a connection string or explicit (key,value) pairs:
 *
 * <pre>{@code
 * db = "mysql://dbuser:secretpassword@localhost:3306/mydb"
 * }</pre>
 *
 * <p>Key/value pairs:
 *
 * <pre>{@code
 * db.host = localhost
 * db.port = 3306
 * db.database = mydb
 * db.user = dbuser
 * db.password = secretpassword
 * }</pre>
 *
 * <p>There is an internal Verticle (one per IO threads) with a dedicated SqlConnection. This
 * connection is only accessible from a Vertx thread any attempt to access to the connection from a
 * non Vertx thread will result in exception.
 *
 * <p>Same applies for the {@link io.vertx.sqlclient.PreparedStatement}/{@link
 * io.vertx.sqlclient.PreparedQuery} instances.
 *
 * @author edgar
 * @since 4.0.8
 */
public class VertxMySQLConnectionModule extends VertxSqlConnectionModule {

  /**
   * Creates a mysql connection using the provided name as key.
   *
   * @param name Name of the configuration property to read. Can be a connection uri or a json
   *     object like.
   */
  public VertxMySQLConnectionModule(String name) {
    super(name);
  }

  /**
   * Creates a mysql connection. On application configuration there must be a <code>db</code> entry.
   */
  public VertxMySQLConnectionModule() {
    this("db");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected void install(Jooby application, String name, SqlConnectOptions options) {
    var registry = application.getServices();
    var pgConnection = new VertxMySQLConnectionProxy(options.getDatabase());
    var provider = new VertxSqlClientProvider(options.getDatabase());
    registry.put(ServiceKey.key(MySQLConnection.class, name), pgConnection);
    registry.put(ServiceKey.key(SqlClientInternal.class, name), provider);

    registry.putIfAbsent(ServiceKey.key(MySQLConnection.class), pgConnection);
    registry.putIfAbsent(ServiceKey.key(SqlClientInternal.class), provider);
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
  protected Deployable newSqlClient(
      SqlConnectOptions options, Map<String, List<String>> preparedStatements) {
    return new VertxSqlConnectionVerticle<>(
        MySQLConnection::connect, (MySQLConnectOptions) options, preparedStatements);
  }
}
