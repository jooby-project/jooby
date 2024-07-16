/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3474;

import static org.junit.jupiter.api.Assertions.*;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3474 {

  @ServerTest
  public void shouldPropagatePathVarsOnMount(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mount("/{id:(foo|bar)}", new App3474());
            })
        .ready(
            http -> {
              http.get(
                  "/foo",
                  rsp -> {
                    assertEquals("foo", rsp.body().string());
                  });
            });
  }
}
