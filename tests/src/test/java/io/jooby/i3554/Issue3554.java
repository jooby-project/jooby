/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3554;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.jooby.ExecutionMode;
import io.jooby.ReactiveSupport;
import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3554 {
  @ServerTest(executionMode = ExecutionMode.EVENT_LOOP)
  public void shouldNotThrowErrorOnCompletableWithSideEffect(ServerTestRunner runner) {
    runner
        .options(new ServerOptions().setPort(9000).setDefaultHeaders(false))
        .define(
            app -> {
              ExecutorService threadPool = Executors.newSingleThreadExecutor();

              app.use(ReactiveSupport.concurrent());

              app.onStop(threadPool::shutdown);

              app.get(
                  "/3554",
                  ctx ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try (var os = ctx.responseStream();
                                var bos = new BufferedOutputStream(os)) {
                              bos.write("test".getBytes(StandardCharsets.UTF_8));
                            } catch (Exception ex) {
                              throw new RuntimeException(ex);
                            }
                            return ctx;
                          },
                          threadPool));
            })
        .ready(
            http -> {
              http.get(
                  "/3554",
                  rsp -> {
                    assertEquals("test", rsp.body().string());
                    assertEquals(200, rsp.code());
                  });
            });
  }
}
