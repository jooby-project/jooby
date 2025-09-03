/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.MediaType.JSON;

import java.util.List;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.StartupSummary;
import io.jooby.netty.NettyServer;

public class Performance extends Jooby {

  private static final String MESSAGE = "Hello, World!";

  {
    var outputFactory = getOutputFactory();
    var message = outputFactory.wrap(MESSAGE);
    setStartupSummary(List.of(StartupSummary.VERBOSE));
    get(
        "/plaintext",
        ctx -> {
          return ctx.send(message);
        });

    get("/json", ctx -> ctx.setResponseType(JSON).render(new Message(MESSAGE)));

    get(
        "/debug",
        ctx -> {
          getLog().info("Info");
          return ctx.send(message);
        });
  }

  public static void main(final String[] args) {
    System.setProperty("io.netty.disableHttpHeadersValidation", "true");
    runApp(args, new NettyServer(new ServerOptions().setPort(3001)), EVENT_LOOP, Performance::new);
  }
}
