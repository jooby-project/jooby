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

/**
 * A reactive SQL Client for mySQL. See https://vertx.io/docs/vertx-mysql-client/java/.
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
 * The default constructor creates a {@link MySQLBuilder#pool()}. To use the client version:
 *
 * <pre>{@code
 * install(new VertxMySQLModule(MySQLBuilder::client));
 * }</pre>
 *
 * @author edgar
 * @since 4.0.8
 */
public class VertxMySQLModule extends VertxSqlClientModule {

  private final Supplier<ClientBuilder<? extends SqlClient>> builder;

  /**
   * Creates a new vertx mysql module.
   *
   * @param name Database key.
   * @param builder Client supplier.
   */
  public VertxMySQLModule(
      @NonNull String name, @NonNull Supplier<ClientBuilder<? extends SqlClient>> builder) {
    super(name);
    this.builder = builder;
  }

  /**
   * Creates a new vertx mysql module.
   *
   * @param builder Client supplier.
   */
  public VertxMySQLModule(@NonNull Supplier<ClientBuilder<? extends SqlClient>> builder) {
    this("db", builder);
  }

  /** Creates a default client using a pool client. */
  public VertxMySQLModule() {
    this(MySQLBuilder::pool);
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
  protected ClientBuilder<? extends SqlClient> newBuilder() {
    return builder.get();
  }
}
