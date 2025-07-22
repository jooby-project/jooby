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
import io.jooby.netty.NettyServer;

public class Performance extends Jooby {

  private static final String MESSAGE = "Hello, World!";

  private static final byte[] MESSAGE_BYTES = MESSAGE.getBytes(StandardCharsets.UTF_8);

  {
    var outputFactory = getOutputFactory();
    var message = outputFactory.wrap(MESSAGE_BYTES);
    get(
        "/plaintext",
        ctx -> {
          // return ctx.send(outputFactory.wrap(MESSAGE_BYTES));
          return ctx.send(message);
        });

    get("/json", ctx -> ctx.setResponseType(JSON).render(new Message(MESSAGE)));

    get("/db", ctx -> ctx.send(StatusCode.OK));

    get("/queries", ctx -> ctx.send(StatusCode.OK));

    get("/fortunes", ctx -> ctx.send(StatusCode.OK));

    get("/updates", ctx -> ctx.send(StatusCode.OK));
  }

  public static void main(final String[] args) {
    System.setProperty("io.netty.disableHttpHeadersValidation", "true");
    // runApp(args, new
    // UndertowServer().setOutputFactory(OutputFactory.threadLocal(OutputFactory.create())),
    // EVENT_LOOP, Performance::new);
    runApp(args, new NettyServer(), EVENT_LOOP, Performance::new);
  }
}
