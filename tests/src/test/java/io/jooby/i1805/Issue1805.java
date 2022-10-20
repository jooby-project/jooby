/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i1805;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1805 {
  @ServerTest
  public void shouldParseURLLikeParam(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C1805());
            })
        .ready(
            client -> {
              client.get(
                  "/1805/uri?param=https://jooby.io",
                  rsp -> {
                    assertEquals("https://jooby.io", rsp.body().string());
                  });
              client.get(
                  "/1805/url?param=https://jooby.io",
                  rsp -> {
                    assertEquals("https://jooby.io", rsp.body().string());
                  });
            });
  }
}
