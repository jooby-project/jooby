/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3084;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3084 {

  @ServerTest
  public void shouldSupportParamInNonAsciiRouter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get("/3084/tést/{param}", ctx -> ctx.path("param").value());
              app.get("/3084/régex/{r:\\d+}", ctx -> ctx.path("r").value());
              app.get("/3084/préfix/prefix-{p}", ctx -> ctx.path("p").value());
              app.get("/3084/súffix/{p}-suffix", ctx -> ctx.path("p").value());
              app.get("/3084/táil/*", ctx -> ctx.path("*").value());
              app.get("/3084/táil-with-name/*path", ctx -> ctx.path("path").value());
            })
        .ready(
            http -> {
              http.get(
                  "/3084/tést/1234",
                  rsp -> {
                    assertEquals("1234", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/tést/fóo",
                  rsp -> {
                    assertEquals("fóo", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/régex/909",
                  rsp -> {
                    assertEquals("909", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/préfix/prefix-foo",
                  rsp -> {
                    assertEquals("foo", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/súffix/foo-suffix",
                  rsp -> {
                    assertEquals("foo", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/táil/some/path",
                  rsp -> {
                    assertEquals("some/path", rsp.body().string().trim());
                  });
              http.get(
                  "/3084/táil-with-name/some/path",
                  rsp -> {
                    assertEquals("some/path", rsp.body().string().trim());
                  });
            });
  }
}
