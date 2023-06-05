/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2525;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2525 {
  @ServerTest
  public void shouldHandleMultipleAcceptHeaders(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.get(
                  "/2525",
                  ctx -> {
                    return ctx.query("foo").toList(Foo2525.class);
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/2525",
                  rsp -> {
                    assertEquals("[]", rsp.body().string());
                  });
              http.get(
                  "/2525?something=else",
                  rsp -> {
                    assertEquals("[]", rsp.body().string());
                  });
              http.get(
                  "/2525?foo[0][a]=10&foo[0][b]=20&foo[1][a]=30&foo[1][b]=40&something=else",
                  rsp -> {
                    assertEquals("[{\"a\":10,\"b\":20},{\"a\":30,\"b\":40}]", rsp.body().string());
                  });
            });
  }
}
