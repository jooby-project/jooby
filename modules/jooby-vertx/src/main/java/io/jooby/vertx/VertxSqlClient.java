/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PreparedStatement;

public abstract class VertxSqlClient implements Extension {

  private static final BiConsumer<VertxPreparedStatement, PreparedStatement> NOOP =
      (name, stt) -> {};

  private final List<VertxPreparedStatement> preparedStatements = new ArrayList<>();

  private BiConsumer<VertxPreparedStatement, PreparedStatement> onPreparedStatement = NOOP;

  public VertxSqlClient onPreparedStatement(
      @NonNull BiConsumer<VertxPreparedStatement, PreparedStatement> callback) {
    this.onPreparedStatement = callback;
    return this;
  }

  public VertxSqlClient prepare(@NonNull VertxPreparedStatement preparedStatement) {
    preparedStatements.add(preparedStatement);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    var registry = application.getServices();
    var instances = application.getServerOptions().getIoThreads();
    var vertx = registry.getOrNull(Vertx.class);
    if (vertx == null) {
      application.onStarting(
          () ->
              deploySqlClient(
                  application.require(Vertx.class),
                  instances,
                  preparedStatements,
                  onPreparedStatement));
    } else {
      deploySqlClient(vertx, instances, preparedStatements, onPreparedStatement);
    }
  }

  private void deploySqlClient(
      Vertx vertx,
      int instances,
      List<VertxPreparedStatement> preparedStatements,
      BiConsumer<VertxPreparedStatement, PreparedStatement> callback) {
    var options = new DeploymentOptions().setInstances(instances);
    vertx.deployVerticle(() -> newSqlClient(preparedStatements, callback), options);
  }

  protected abstract Deployable newSqlClient(
      List<VertxPreparedStatement> preparedStatements,
      BiConsumer<VertxPreparedStatement, PreparedStatement> callback);
}
