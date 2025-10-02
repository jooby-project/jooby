/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import java.util.List;

import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class DbController {
  @Inject
  public DbController(
      PgConnection connection,
      @Named("selectWorld") PreparedStatement selectWorld,
      @Named("updateWorld") List<PreparedStatement> updateWorlds,
      @Named("updateWorld") List<PreparedQuery<RowSet<Row>>> updateWorldsQuery) {}
}
