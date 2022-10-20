/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.FormBody;

public class Issue1687 {

  @ServerTest
  public void shouldUseHiddenMethod(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setHiddenMethod("_method");
              app.put("/1687", Context::getMethod);
            })
        .ready(
            client -> {
              client.post(
                  "/1687",
                  new FormBody.Builder().add("_method", "put").build(),
                  rsp -> {
                    assertEquals("PUT", rsp.body().string());
                  });

              // If _method is missing, then 405
              client.post(
                  "/1687",
                  rsp -> {
                    assertEquals(StatusCode.METHOD_NOT_ALLOWED.value(), rsp.code());
                  });
            });
  }

  @ServerTest
  public void shouldUseHiddenMethodFromStrategy(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.setHiddenMethod(ctx -> ctx.header("X-HTTP-Method-Override").toOptional());
              app.put("/1687", Context::getMethod);
            })
        .ready(
            client -> {
              client.header("X-HTTP-Method-Override", "put");
              client.post(
                  "/1687",
                  rsp -> {
                    assertEquals("PUT", rsp.body().string());
                  });
            });
  }
}
