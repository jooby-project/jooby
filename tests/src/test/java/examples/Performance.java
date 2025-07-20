/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.MediaType.JSON;

import java.nio.charset.StandardCharsets;

import io.jooby.Jooby;
import io.jooby.StatusCode;
import io.jooby.undertow.UndertowServer;

public class Performance extends Jooby {

  private static final String MESSAGE = "Hello, World!";

  private static final byte[] MESSAGE_BYTES = MESSAGE.getBytes(StandardCharsets.UTF_8);

  {
    var message = getOutputFactory().wrap(MESSAGE_BYTES);
    get(
        "/plaintext",
        ctx -> {
          return ctx.send(message);
        });

    get("/json", ctx -> ctx.setResponseType(JSON).render(new Message(MESSAGE)));

    get("/db", ctx -> ctx.send(StatusCode.OK));

    get("/queries", ctx -> ctx.send(StatusCode.OK));

    get("/fortuxnes", ctx -> ctx.send(StatusCode.OK));

    get("/updates", ctx -> ctx.send(StatusCode.OK));
  }

  public static void main(final String[] args) {
    System.setProperty("io.netty.disableHttpHeadersValidation", "true");
    runApp(args, new UndertowServer(), EVENT_LOOP, Performance::new);
  }
}
