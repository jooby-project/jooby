/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2529;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2529 {
  @ServerTest
  public void shouldHandleMultipleAcceptHeaders(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new Controller2529());
            })
        .ready(
            http -> {
              http.header("Accept", "text/plain")
                  .header("Accept", "application/json")
                  .get(
                      "/2529",
                      rsp -> {
                        assertEquals("Hello world", rsp.body().string());
                      });
            });
  }
}
