/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import java.util.List;
import java.util.function.BiConsumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.internal.vertx.VertxSqlConnectionProvider;
import io.jooby.internal.vertx.VertxSqlConnectionVerticle;
import io.vertx.core.*;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.PreparedStatement;

public class VertxPgClient extends VertxSqlClient {

  private final PgConnectOptions options;
  private final String name;

  public VertxPgClient(String name, PgConnectOptions options) {
    this.name = name;
    this.options = options;
  }

  public VertxPgClient(PgConnectOptions options) {
    this("db", options);
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    super.install(application);

    var registry = application.getServices();
    var provider = new VertxSqlConnectionProvider<PgConnection>();
    registry.put(ServiceKey.key(PgConnection.class, name), provider);
    registry.putIfAbsent(ServiceKey.key(PgConnection.class), provider);
  }

  @Override
  protected Deployable newSqlClient(
      List<VertxPreparedStatement> preparedStatements,
      BiConsumer<VertxPreparedStatement, PreparedStatement> callback) {
    return new VertxSqlConnectionVerticle<>(
        PgConnection::connect, options, preparedStatements, callback);
  }
}
