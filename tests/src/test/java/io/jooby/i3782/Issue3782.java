/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3782;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3782 {

  @ServerTest
  public void shouldFollowKeepAliveHeader(ServerTestRunner runner) throws IOException {
    runner
        .define(
            app -> {
              app.get("/3782/keep-alive", Context::getRequestPath);
              app.get("/3782/keep-alive-empty", ctx -> ctx.send(StatusCode.OK));
            })
        .ready(
            http -> {
              // Keep alive by default
              http.get(
                  "/3782/keep-alive",
                  rsp -> {
                    assertEquals("/3782/keep-alive", rsp.body().string());
                    assertNull(rsp.header("Connection"));
                  });
              http.get(
                  "/3782/keep-alive-empty",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertNull(rsp.header("Connection"));
                  });

              // Keep alive by explicit
              http.header("connection", "keep-alive");
              http.get(
                  "/3782/keep-alive",
                  rsp -> {
                    assertEquals("/3782/keep-alive", rsp.body().string());
                    assertNull(rsp.header("Connection"));
                  });
              http.header("connection", "keep-alive");
              http.get(
                  "/3782/keep-alive-empty",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertNull(rsp.header("Connection"));
                  });

              // Close
              http.header("connection", "close");
              http.get(
                  "/3782/keep-alive",
                  rsp -> {
                    assertEquals("/3782/keep-alive", rsp.body().string());
                    assertEquals("close", rsp.header("Connection").toLowerCase());
                  });
              http.header("connection", "close");
              http.get(
                  "/3782/keep-alive-empty",
                  rsp -> {
                    assertEquals("", rsp.body().string());
                    assertEquals("close", rsp.header("Connection").toLowerCase());
                  });
            });
  }
}
