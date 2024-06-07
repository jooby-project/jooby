/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1859;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1859 {

  @ServerTest
  public void shouldNotGetEmptyStringOnMissingEmptyBody(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C1859_());
              app.post("/i1859", ctx -> ctx.body().value("empty"));
            })
        .ready(
            http -> {
              http.post(
                  "/c/i1859",
                  rsp -> {
                    assertEquals("empty", rsp.body().string());
                  });
              http.post(
                  "/i1859",
                  rsp -> {
                    assertEquals("empty", rsp.body().string());
                  });
            });
  }
}
