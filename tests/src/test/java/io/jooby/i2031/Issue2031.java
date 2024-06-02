/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2031;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2031 {
  @ServerTest
  public void shouldWorkWithCompletableFuture(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2031_());
            })
        .ready(
            http -> {
              http.get(
                  "/i2031/completableFuture",
                  rsp -> assertEquals("Hello CompletableFuture", rsp.body().string()));
            });
  }

  @ServerTest
  public void shouldWorkWithRxJava(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2031_());
            })
        .ready(
            http -> {
              http.get("/i2031/single", rsp -> assertEquals("Hello Single", rsp.body().string()));
            });
  }

  @ServerTest
  public void shouldWorkWithReactor(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2031());
            })
        .ready(
            http -> {
              http.get("/i2031/mono", rsp -> assertEquals("Hello Mono", rsp.body().string()));
            });
  }

  @ServerTest
  public void shouldWorkWithMutiny(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new C2031_());
            })
        .ready(
            http -> {
              http.get("/i2031/uni", rsp -> assertEquals("Hello Uni", rsp.body().string()));
            });
  }
}
