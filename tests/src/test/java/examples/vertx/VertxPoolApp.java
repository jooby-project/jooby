/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import static io.jooby.MediaType.JSON;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.Jooby;
import io.jooby.netty.NettyServer;
import io.jooby.vertx.VertxModule;
import io.jooby.vertx.pgclient.VertxPgModule;
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
    install(new VertxModule());
    install(VertxPgModule.client());

    get(
        "/db",
        ctx -> {
          require(SqlClient.class)
              .preparedQuery("SELECT id, randomnumber from WORLD where id=$1")
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
  }

  public static void main(String[] args) {
    runApp(args, new NettyServer(), VertxPoolApp::new);
  }
}
