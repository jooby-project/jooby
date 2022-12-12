/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2465;

import static io.jooby.ReactiveSupport.concurrent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.WebClient;

public class Issue2465PipelineOrder {

  @ServerTest
  public void shouldMakeSureAfterIsNotCallItTwice(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.before(
                  ctx -> {
                    ctx.setResponseHeader("pipeline", "before,");
                  });

              app.after(
                  (ctx, result, failure) -> {
                    ctx.setResponseHeader("pipeline", ctx.getResponseHeader("pipeline") + "after");
                  });

              app.use(concurrent());

              app.get(
                  "/2465",
                  ctx -> {
                    return CompletableFuture.supplyAsync(
                        () -> {
                          ctx.setResponseHeader(
                              "pipeline", ctx.getResponseHeader("pipeline") + "route,");
                          return ctx.getRequestPath();
                        });
                  });
            })
        .ready(
            (WebClient http) -> {
              http.get(
                  "/2465",
                  rsp -> {
                    assertEquals("before,route,after", rsp.header("pipeline"));
                  });
            });
  }
}
