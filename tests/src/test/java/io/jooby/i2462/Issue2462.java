/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2462;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.utow.UndertowServer;

public class Issue2462 {
  @ServerTest(server = UndertowServer.class)
  public void shouldSupportsSse(ServerTestRunner runner) {
    char[] array = new char[3048];
    Arrays.fill(array, 'A');
    int messages = 100;
    String message = new String(array);
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.sse(
                  "/",
                  sse -> {
                    for (int i = 0; i < 10000; i++) {
                      sse.send(message);
                    }
                    // sse.send("message 2");
                  });
            })
        .ready(
            client -> {
              client
                  .sse("/")
                  .next(
                      msg -> {
                        assertEquals(message, msg.getData());
                      })
                  .verify();
            });
  }
}
