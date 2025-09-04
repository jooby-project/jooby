/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.MediaType.JSON;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.StartupSummary;
import io.jooby.jetty.JettyServer;

public class Performance extends Jooby {

  private static final String MESSAGE = "Hello, World!";

  {
    var outputFactory = getOutputFactory();
    var message = outputFactory.wrap(MESSAGE.getBytes(StandardCharsets.UTF_8));
    setStartupSummary(List.of(StartupSummary.VERBOSE));
    getLog().info("processors {}", Runtime.getRuntime().availableProcessors());
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
    runApp(args, new JettyServer(new ServerOptions()), EVENT_LOOP, Performance::new);
  }
}
