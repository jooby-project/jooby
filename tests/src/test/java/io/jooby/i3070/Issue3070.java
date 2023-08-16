/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3070;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.Context;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3070 {

  @ServerTest
  public void shouldServeNonAsciiFileNames(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.assets("/3070/static/*", "/io/jooby/i3070");

              app.get("/3070/tést", Context::getRequestPath);
              app.get("/3070/param/{param}", ctx -> ctx.path("param").value());
            })
        .ready(
            http -> {
              http.get(
                  "/3070/static/test.txt",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("ASCII", rsp.body().string().trim());
                  });
              http.get(
                  "/3070/static/tést.txt",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("NónASCII", rsp.body().string().trim());
                  });
              http.get(
                  "/3070/tést",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("/3070/t%C3%A9st", rsp.body().string().trim());
                  });
              http.get(
                  "/3070/param/hóla",
                  rsp -> {
                    assertEquals(200, rsp.code());
                    assertEquals("hóla", rsp.body().string().trim());
                  });
            });
  }
}
