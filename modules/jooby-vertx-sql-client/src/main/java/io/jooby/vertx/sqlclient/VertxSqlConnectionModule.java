/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.sqlclient;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.internal.vertx.sqlclient.VertxPreparedQueryProxy;
import io.jooby.internal.vertx.sqlclient.VertxPreparedQueryProxyList;
import io.jooby.internal.vertx.sqlclient.VertxPreparedStatementProxy;
import io.jooby.internal.vertx.sqlclient.VertxPreparedStatementProxyList;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public abstract class VertxSqlConnectionModule implements Extension {

  @SuppressWarnings("unchecked")
  private static final Reified<PreparedQuery<RowSet<Row>>> PreparedQueryType =
      (Reified<PreparedQuery<RowSet<Row>>>)
          Reified.getParameterized(
              PreparedQuery.class, Reified.getParameterized(RowSet.class, Row.class).getType());

  private static final Reified<List<PreparedQuery<RowSet<Row>>>> PreparedQueryTypeList =
      Reified.list(PreparedQueryType.getType());

  private Map<String, List<String>> preparedStatements = Map.of();

  public VertxSqlConnectionModule prepare(@NonNull Map<String, List<String>> statements) {
    preparedStatements = statements;
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var registry = application.getServices();
    // Allow to get a prepared statement reference, which only works from a Vert.x thread
    for (var name : preparedStatements.keySet()) {
      // Prepared statements
      registry.put(
          ServiceKey.key(Reified.list(PreparedStatement.class), name),
          new VertxPreparedStatementProxyList(name));
      registry.put(
          ServiceKey.key(PreparedStatement.class, name), new VertxPreparedStatementProxy(name));
      // Prepared queries
      registry.put(
          ServiceKey.key(PreparedQueryTypeList, name),
          new VertxPreparedQueryProxyList(name + ".query"));
      registry.put(
          ServiceKey.key(PreparedQueryType, name), new VertxPreparedQueryProxy(name + ".query"));
    }
    var instances = application.getServerOptions().getIoThreads();
    var vertx = registry.require(Vertx.class);
    var options = new DeploymentOptions().setInstances(instances);
    var connection = vertx.deployVerticle(() -> newSqlClient(preparedStatements), options);
    // wait for success or fail
    connection.await();
  }

  protected abstract Deployable newSqlClient(Map<String, List<String>> preparedStatements);
}
