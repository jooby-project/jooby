/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2408;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.WebClient;

public class Issue2408 {
  @ServerTest
  public void shouldNotIgnoreAnnotationOnParam(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2408_());
              app.error(
                  (ctx, cause, code) -> {
                    ctx.send(cause.getMessage());
                  });
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2408/nullable?blah=stuff",
                  rsp -> {
                    assertEquals("nothing", rsp.body().string());
                  });

              http.get(
                  "/2408/nonnull",
                  rsp -> {
                    assertEquals("Missing value: 'name'", rsp.body().string());
                  });
            });
  }
}
