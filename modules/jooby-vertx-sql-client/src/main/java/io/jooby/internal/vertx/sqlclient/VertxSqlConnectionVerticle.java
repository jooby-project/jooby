/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

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
  private final Map<String, List<String>> preparedStatements;
  private Connection connection;

  public VertxSqlConnectionVerticle(
      BiFunction<Vertx, Options, Future<Connection>> factory,
      Options options,
      Map<String, List<String>> preparedStatements) {
    this.factory = factory;
    this.options = options;
    this.preparedStatements = preparedStatements;
  }

  @Override
  public Future<?> stop() {
    return connection.close();
  }

  @Override
  public Future<?> start() {
    return factory
        .apply(vertx, options)
        .transform(
            ar -> {
              if (ar.succeeded()) {
                this.connection = ar.result();
                VertxThreadLocalSqlConnection.set(options.getDatabase(), connection);
                var futures = new ArrayList<Future<PreparedStatement>>();
                // init prepared statements.
                preparedStatements.forEach(
                    (key, sqlList) -> {
                      var compiled = new TreeMap<Integer, PreparedStatement>();
                      var size = sqlList.size();
                      for (var i = 0; i < size; i++) {
                        var order = i;
                        futures.add(
                            connection
                                .prepare(sqlList.get(i))
                                .onSuccess(
                                    result -> {
                                      compiled.put(order, result);
                                      if (compiled.size() == size) {
                                        var statementList = compiled.values().stream().toList();
                                        var queryList =
                                            statementList.stream()
                                                .map(PreparedStatement::query)
                                                .toList();
                                        // bind to thread
                                        VertxThreadLocalPreparedObject.set(key, statementList);
                                        VertxThreadLocalPreparedObject.set(
                                            key + ".query", queryList);
                                      }
                                    }));
                      }
                    });
                return Future.all(futures);
              } else {
                return Future.failedFuture(ar.cause());
              }
            });
  }
}
