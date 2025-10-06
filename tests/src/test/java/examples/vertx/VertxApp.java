/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.vertx.VertxHandler.vertx;

import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import io.jooby.Jooby;
import io.jooby.netty.NettyServer;
import io.jooby.vertx.VertxModule;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;

public class VertxApp extends Jooby {

  {
    install(new VertxModule());
    var eb = require(EventBus.class);
    eb.consumer(
        "news.uk.sport",
        message -> {
          getLog().info("I have received a message: {}", message.body());
        });

    use(vertx());

    get(
        "/publish",
        ctx -> {
          ctx.require(EventBus.class)
              .publish(
                  "news.uk.sport",
                  Map.of("msg", ctx.query("msg").value(UUID.randomUUID().toString())));
          return "Sent";
        });

    get(
        "/readme",
        ctx -> {
          var fs = ctx.require(FileSystem.class);
          return fs.open(
              Paths.get(System.getProperty("user.dir"), "pom.xml").toAbsolutePath().toString(),
              new OpenOptions());
        });
  }

  public static void main(String[] args) {
    runApp(args, new NettyServer(), EVENT_LOOP, VertxApp::new);
  }
}
