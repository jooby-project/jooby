/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3756;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3756 {
  @ServerTest
  public void shouldCompileConflictConstructor(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C3756_(s -> {}));
            })
        .ready(
            http -> {
              http.get(
                  "/3756",
                  rsp -> {
                    assertEquals("hello", rsp.body().string());
                  });
            });
  }
}
