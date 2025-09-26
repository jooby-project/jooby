/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.jooby.vertx.VertxPreparedStatement;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;

public class VertxSqlConnectionVerticle<
        Connection extends SqlConnection, Options extends SqlConnectOptions>
    extends VerticleBase {

  private final BiFunction<Vertx, Options, Future<Connection>> factory;
  private final Options options;
  private final List<VertxPreparedStatement> preparedStatements;
  private final BiConsumer<VertxPreparedStatement, PreparedStatement> callback;

  public VertxSqlConnectionVerticle(
      BiFunction<Vertx, Options, Future<Connection>> factory,
      Options options,
      List<VertxPreparedStatement> preparedStatements,
      BiConsumer<VertxPreparedStatement, PreparedStatement> callback) {
    this.factory = factory;
    this.options = options;
    this.preparedStatements = preparedStatements;
    this.callback = callback;
  }

  @Override
  public Future<?> start() {
    return factory
        .apply(vertx, options)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                var connection = ar.result();
                VertxSqlConnectionProvider.set(connection);
                // init prepared statements.
                preparedStatements.forEach(
                    ps -> {
                      connection
                          .prepare(ps.sql())
                          .onComplete(
                              ar2 -> {
                                if (ar2.succeeded()) {
                                  var statement = ar2.result();
                                  VertxThreadLocalPreparedStatement.set(statement);
                                  callback.accept(ps, VertxThreadLocalPreparedStatement.INSTANCE);
                                }
                              });
                    });
              }
            });
  }
}
