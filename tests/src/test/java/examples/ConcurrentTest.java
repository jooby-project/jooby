/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.*;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class ConcurrentTest {

  @ServerTest
  public void makeSureBufferWork(ServerTestRunner runner) {
    var payload = "Hello World!";
    runner
        .define(
            app -> {
              var outputFactory = app.getOutputFactory();
              var message = outputFactory.wrap(payload);

              app.get(
                  "/plaintext",
                  ctx -> {
                    return ctx.send(message);
                  });
            })
        .ready(
            http -> {
              http.get("/plaintext").execute(50, rsp -> assertEquals(payload, rsp.body().string()));
            });
  }
}
