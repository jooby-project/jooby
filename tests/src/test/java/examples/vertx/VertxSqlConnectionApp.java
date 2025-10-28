/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.MediaType.JSON;
import static io.jooby.Reified.getParameterized;
import static java.util.stream.IntStream.range;

import java.util.*;
import java.util.function.Consumer;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.*;
import io.jooby.guice.GuiceModule;
import io.jooby.vertx.VertxServer;
import io.jooby.vertx.pgclient.VertxPgConnectionModule;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.SqlClientInternal;
import io.vertx.sqlclient.internal.SqlClientBase;

public class VertxSqlConnectionApp extends Jooby {
  private static final String SELECT_WORLD = "SELECT id, randomnumber from WORLD where id=$1";
  private static final String SELECT_FORTUNE = "SELECT id, message from FORTUNE";

  private static final Reified<PreparedQuery<RowSet<Row>>> PreparedQueryType =
      getParameterized(PreparedQuery.class, getParameterized(RowSet.class, Row.class));

  private static final Reified<List<PreparedQuery<RowSet<Row>>>> PreparedQueryTypeList =
      Reified.list(PreparedQueryType);

  private final PreparedQuery<RowSet<Row>> selectWorldQuery;
  private final List<PreparedQuery<RowSet<Row>>> updateWorldQuery;
  private final SqlClientInternal sqlClient;

  {
    getEnvironment()
        .setConfig(
            getConfig()
                .withFallback(
                    ConfigFactory.empty()
                        .withValue("db.host", ConfigValueFactory.fromAnyRef("localhost"))
                        .withValue("db.port", ConfigValueFactory.fromAnyRef(5432))
                        .withValue("db.pipeliningLimit", ConfigValueFactory.fromAnyRef(16))
                        .withValue("db.database", ConfigValueFactory.fromAnyRef("hello_world"))
                        .withValue("db.user", ConfigValueFactory.fromAnyRef("benchmarkdbuser"))
                        .withValue(
                            "db.password", ConfigValueFactory.fromAnyRef("benchmarkdbpass"))));

    install(new VertxPgConnectionModule().prepare(statements()));
    install(new GuiceModule());

    this.selectWorldQuery = require(PreparedQueryType, "selectWorld");
    this.updateWorldQuery = require(PreparedQueryTypeList, "updateWorld");
    this.sqlClient = require(SqlClientBase.class);

    onStarted(() -> require(DbController.class));

    get(
        "/db",
        ctx -> {
          selectWorldQuery
              .execute(Tuple.of(Util.boxedRandomWorld()))
              .onComplete(
                  rsp -> {
                    if (rsp.succeeded()) {
                      var rs = rsp.result().iterator();
                      var row = rs.next();
                      ctx.setResponseType(JSON)
                          .send(Json.encode(new World(row.getInteger(0), row.getInteger(1))));
                    } else {
                      ctx.sendError(rsp.cause());
                    }
                  });
          return ctx;
        });

    /* Multiple queries: */
    get(
        "/queries",
        ctx -> {
          int queries = Util.queries(ctx);
          selectWorlds(ctx, queries, result -> ctx.setResponseType(JSON).send(Json.encode(result)));
          return ctx;
        });

    get(
        "/updates",
        ctx -> {
          int queries = Util.queries(ctx);
          selectWorlds(
              ctx,
              queries,
              result -> {
                updateWorld(
                    result,
                    ar -> {
                      if (ar.failed()) {
                        sendError(ctx, ar.cause());
                      } else {
                        ctx.setResponseType(JSON).send(Json.encode(result));
                      }
                    });
              });
          return ctx;
        });
  }

  public void selectWorlds(Context ctx, int queries, Consumer<List<World>> consumer) {
    sqlClient.group(
        client -> {
          var statement = client.preparedQuery(SELECT_WORLD);
          List<World> worlds = new ArrayList<>(queries);
          for (int i = 0; i < queries; i++) {
            statement
                .execute(Tuple.of(Util.boxedRandomWorld()))
                .map(rs -> new World(rs.iterator().next().getInteger(0), Util.boxedRandomWorld()))
                .onComplete(
                    ar -> {
                      if (ar.succeeded()) {
                        worlds.add(ar.result());
                        if (worlds.size() == queries) {
                          consumer.accept(worlds);
                        }
                      } else {
                        sendError(ctx, ar.cause());
                      }
                    });
          }
        });
  }

  public void updateWorld(List<World> worlds, Handler<AsyncResult<RowSet<Row>>> handler) {
    Collections.sort(worlds);
    int len = worlds.size();
    List<Object> arguments = new ArrayList<>();
    for (var world : worlds) {
      arguments.add(world.getId());
      arguments.add(world.getRandomNumber());
    }
    updateWorldQuery.get(len - 1).execute(Tuple.wrap(arguments)).onComplete(handler);
  }

  private void sendError(Context ctx, Throwable cause) {
    if (!ctx.isResponseStarted()) {
      ctx.sendError(cause);
    }
  }

  private Map<String, List<String>> statements() {
    return Map.of(
        "selectWorld", List.of(SELECT_WORLD),
        "selectFortune", List.of(SELECT_FORTUNE),
        "updateWorld",
            range(0, 500).map(i -> i + 1).mapToObj(this::buildAggregatedUpdateQuery).toList());
  }

  private String buildAggregatedUpdateQuery(int len) {
    var sql = new StringBuilder();
    sql.append("UPDATE WORLD SET RANDOMNUMBER = CASE ID");
    for (int i = 0; i < len; i++) {
      int offset = (i * 2) + 1;
      sql.append(" WHEN $").append(offset).append(" THEN $").append(offset + 1);
    }
    sql.append(" ELSE RANDOMNUMBER");
    sql.append(" END WHERE ID IN ($1");
    for (int i = 1; i < len; i++) {
      int offset = (i * 2) + 1;
      sql.append(",$").append(offset);
    }
    sql.append(")");
    return sql.toString();
  }

  public static void main(String[] args) {
    runApp(
        args,
        new VertxServer().setOptions(new ServerOptions().setIoThreads(2)),
        EVENT_LOOP,
        VertxSqlConnectionApp::new);
  }
}
