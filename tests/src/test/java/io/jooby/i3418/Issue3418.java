/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3418;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3418 {

  @ServerTest
  public void shouldInstallWithPredicate(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(ctx -> ctx.header("v").value("").equals("1.0"), Foo3418::new);
              app.install(ctx -> ctx.header("v").value("").equals("2.0"), Bar3418::new);
              app.get("/app", ctx -> "App");
            })
        .ready(
            http -> {
              http.header("v", "1.0")
                  .get(
                      "/app",
                      rsp -> {
                        assertEquals("Foo3418", rsp.body().string());
                      });

              http.header("v", "2.0")
                  .get(
                      "/app",
                      rsp -> {
                        assertEquals("Bar3418", rsp.body().string());
                      });

              http.header("v", "somethingElse")
                  .get(
                      "/app",
                      rsp -> {
                        assertEquals("App", rsp.body().string());
                      });

              http.get(
                  "/app",
                  rsp -> {
                    assertEquals("App", rsp.body().string());
                  });
            });
  }
}
