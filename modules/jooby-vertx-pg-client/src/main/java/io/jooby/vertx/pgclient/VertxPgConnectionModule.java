/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.vertx.pgclient;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.internal.vertx.pgclient.VertxPgConnectionProxy;
import io.jooby.internal.vertx.sqlclient.VertxSqlClientProvider;
import io.jooby.internal.vertx.sqlclient.VertxSqlConnectionVerticle;
import io.jooby.vertx.sqlclient.VertxSqlConnectionModule;
import io.vertx.core.Deployable;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.impl.SqlClientInternal;

public class VertxPgConnectionModule extends VertxSqlConnectionModule {

  private final PgConnectOptions options;
  private final String name;

  public VertxPgConnectionModule(String name, PgConnectOptions options) {
    this.name = name;
    this.options = options;
  }

  public VertxPgConnectionModule(PgConnectOptions options) {
    this("db", options);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    super.install(application);

    var registry = application.getServices();
    var pgConnection = new VertxPgConnectionProxy(options.getDatabase());
    var provider = new VertxSqlClientProvider(options.getDatabase());
    registry.put(ServiceKey.key(PgConnection.class, name), pgConnection);
    registry.put(ServiceKey.key(SqlClientInternal.class, name), provider);

    registry.putIfAbsent(ServiceKey.key(PgConnection.class), pgConnection);
    registry.putIfAbsent(ServiceKey.key(SqlClientInternal.class), provider);
  }

  @Override
  protected Deployable newSqlClient(Map<String, List<String>> preparedStatements) {
    return new VertxSqlConnectionVerticle<>(PgConnection::connect, options, preparedStatements);
  }
}
