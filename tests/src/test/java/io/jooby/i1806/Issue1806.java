/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1806;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1806 {
  @ServerTest
  public void shouldNotGetListWithNullValue(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C1806());
              app.get("/1806/s", ctx -> ctx.query("names").toList(String.class));
            })
        .ready(
            client -> {
              client.get(
                  "/1806/s",
                  rsp -> {
                    assertEquals("[]", rsp.body().string());
                  });
              client.get(
                  "/1806/c",
                  rsp -> {
                    assertEquals("[]", rsp.body().string());
                  });
            });
  }
}
