/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.sqlclient;

import java.util.List;
import java.util.Map;

import com.typesafe.config.ConfigValueType;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.internal.vertx.sqlclient.VertxPreparedQueryProxy;
import io.jooby.internal.vertx.sqlclient.VertxPreparedQueryProxyList;
import io.jooby.internal.vertx.sqlclient.VertxPreparedStatementProxy;
import io.jooby.internal.vertx.sqlclient.VertxPreparedStatementProxyList;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.*;

public abstract class VertxSqlConnectionModule implements Extension {

  private static final Reified<PreparedQuery<RowSet<Row>>> PreparedQueryType =
      Reified.getParameterized(
          PreparedQuery.class, Reified.getParameterized(RowSet.class, Row.class).getType());

  private static final Reified<List<PreparedQuery<RowSet<Row>>>> PreparedQueryTypeList =
      Reified.list(PreparedQueryType.getType());

  private Map<String, List<String>> preparedStatements = Map.of();

  private final String name;

  public VertxSqlConnectionModule(String name) {
    this.name = name;
  }

  public VertxSqlConnectionModule() {
    this("db");
  }

  public VertxSqlConnectionModule prepare(@NonNull Map<String, List<String>> statements) {
    preparedStatements = statements;
    return this;
  }

  @Override
  public final void install(@NonNull Jooby application) throws Exception {
    var registry = application.getServices();
    var config = application.getConfig();
    var configOptions = config.getValue(name);
    SqlConnectOptions connectOptions;
    if (configOptions.valueType() == ConfigValueType.STRING) {
      connectOptions = fromUri(config.getString(name));
    } else {
      connectOptions = fromMap(new JsonObject(config.getObject(name).unwrapped()));
    }

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
    var vertx = registry.require(Vertx.class);
    var options =
        new DeploymentOptions().setInstances(application.getServerOptions().getIoThreads());
    var connection =
        vertx.deployVerticle(() -> newSqlClient(connectOptions, preparedStatements), options);

    install(application, name, connectOptions);
    // wait for success or fail
    connection.await();
  }

  protected abstract void install(Jooby application, String key, SqlConnectOptions options);

  protected abstract SqlConnectOptions fromMap(JsonObject config);

  protected abstract SqlConnectOptions fromUri(String uri);

  protected abstract Deployable newSqlClient(
      SqlConnectOptions options, Map<String, List<String>> preparedStatements);
}
