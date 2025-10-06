/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import io.jooby.netty.NettyServer;
import io.jooby.vertx.VertxHandler;
import io.jooby.vertx.VertxModule;
import io.jooby.vertx.pgclient.VertxPgModule;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class VertxPoolApp extends Jooby {

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

    install(new JacksonModule());
    install(new VertxModule());
    install(new VertxPgModule(PgBuilder::pool));

    use(new VertxHandler());

    get(
        "/db",
        ctx -> {
          var db = require(SqlClient.class);
          return db.preparedQuery("SELECT id, randomnumber from WORLD where id=$1")
              .execute(Tuple.of(Util.boxedRandomWorld()))
              .map(
                  result -> {
                    var row = result.iterator().next();
                    return new World(row.getInteger(0), row.getInteger(1));
                  });
        });

    get(
        "/queries",
        ctx -> {
          int queries = Util.queries(ctx);
          var db = require(SqlClient.class);
          Promise<List<World>> promise = Promise.promise();
          var statement = db.preparedQuery("SELECT id, randomnumber from WORLD where id=$1");
          var worlds = new ArrayList<World>(queries);
          for (int i = 0; i < queries; i++) {
            statement
                .execute(Tuple.of(Util.boxedRandomWorld()))
                .onComplete(
                    ar -> {
                      if (ar.succeeded()) {
                        var rs = ar.result();
                        worlds.add(
                            new World(rs.iterator().next().getInteger(0), Util.boxedRandomWorld()));
                        if (worlds.size() == queries) {
                          promise.complete(worlds);
                        }
                      } else {
                        promise.fail(ar.cause());
                      }
                    });
          }
          return promise;
        });
  }

  public static void main(String[] args) throws InterruptedException {
    runApp(args, new NettyServer(), VertxPoolApp::new);
  }
}
