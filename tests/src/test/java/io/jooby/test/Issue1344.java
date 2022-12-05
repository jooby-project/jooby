/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1344 {

  public static class App1344 extends Jooby {
    {
      use(next -> ctx -> "<" + next.apply(ctx) + ">");

      get("/1344", Context::getRequestPath);
    }
  }

  @ServerTest
  @DisplayName("Decorator from composition")
  public void issue1338(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.use(next -> ctx -> "[" + next.apply(ctx) + "]");

              app.mount(new App1344());
            })
        .ready(
            client -> {
              client.get(
                  "/1344",
                  rsp -> {
                    assertEquals("[</1344>]", rsp.body().string());
                  });
            });
  }
}
