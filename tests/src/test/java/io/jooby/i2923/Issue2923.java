/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2923;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2923 {

  @ServerTest
  public void shouldParseJavaRecord(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.get(
                  "/2923",
                  ctx -> {
                    return ctx.query().to(Person2923.class);
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/2923?firstname=Pedro&lastname=Picapiedra",
                  rsp -> {
                    assertEquals(
                        "{\"firstname\":\"Pedro\",\"lastname\":\"Picapiedra\"}",
                        rsp.body().string());
                  });
            });
  }
}
