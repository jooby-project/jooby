/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.MediaType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.jackson.JacksonModule;
import io.jooby.vertx.VertxPgClient;
import io.jooby.vertx.VertxServer;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Tuple;

public class VertxApp extends Jooby {
  private PreparedStatement selectWord;

  {
    var pgOptions =
        new io.vertx.pgclient.PgConnectOptions()
            .setHost("localhost")
            .setPort(5432)
            .setDatabase("hello_world")
            .setUser("benchmarkdbuser")
            .setPassword("benchmarkdbpass");

    install(
        new VertxPgClient(pgOptions)
            .prepare(BenchQueries.SELECT_WORLD)
            .onPreparedStatement(
                (key, preparedStatement) -> {
                  if (key == BenchQueries.SELECT_WORLD) {
                    selectWord = preparedStatement;
                  }
                }));

    install(new JacksonModule());

    var json = require(ObjectMapper.class);

    get(
        "/db",
        ctx -> {
          System.out.println("HTTP: " + Thread.currentThread().getName());
          selectWord
              .query()
              .execute(Tuple.of(Util.boxedRandomWorld()))
              .onComplete(
                  rsp -> {
                    System.out.println("DB CALLBACK: " + Thread.currentThread().getName());
                    if (rsp.succeeded()) {
                      try {
                        var rs = rsp.result().iterator();
                        var row = rs.next();
                        ctx.setResponseType(JSON)
                            .send(
                                json.writeValueAsBytes(
                                    new World(row.getInteger(0), row.getInteger(1))));
                      } catch (Throwable t) {
                        t.printStackTrace();
                      }
                    } else {
                      ctx.sendError(rsp.cause());
                    }
                  });
          return ctx;
        });
  }

  public static void main(String[] args) {
    runApp(
        args,
        new VertxServer().setOptions(new ServerOptions().setIoThreads(2)),
        EVENT_LOOP,
        VertxApp::new);
  }
}
