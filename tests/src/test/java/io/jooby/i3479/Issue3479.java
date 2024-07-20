/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3479;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3479 {
  @ServerTest
  public void shouldRespectLeadingWhiteSpace(ServerTestRunner runner) {
    var data =
        "This is my event.\n"
            + " This second line starts with a leading space.\n"
            + "But not this third one.";
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.sse(
                  "/3479",
                  sse -> {
                    sse.send(data);
                  });
            })
        .ready(
            client -> {
              client
                  .sse("/3479")
                  .next(
                      message -> {
                        assertEquals(data, message.getData());
                      })
                  .verify();
            });
  }
}
