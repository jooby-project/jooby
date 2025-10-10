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

/**
 * A reactive SQL Client for postgresql. See https://vertx.io/docs/vertx-pg-client/java/.
 *
 * <p>Define a connection string or explicit (key,value) pairs:
 *
 * <pre>{@code
 * db = "postgresql://dbuser:secretpassword@localhost:5432/mydb"
 * }</pre>
 *
 * <p>Key/value pairs:
 *
 * <pre>{@code
 * db.host = localhost
 * db.port = 5432
 * db.database = mydb
 * db.user = dbuser
 * db.password = secretpassword
 * }</pre>
 *
 * The default constructor creates a {@link PgBuilder#pool()}. To use the client version:
 *
 * <pre>{@code
 * install(new VertxMySQLModule(PgBuilder::client));
 * }</pre>
 *
 * @author edgar
 * @since 4.0.8
 */
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

  public VertxPgModule() {
    this(PgBuilder::pool);
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
