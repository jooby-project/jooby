/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3607;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jooby.FlashMap;
import io.jooby.ModelAndView;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pebble.PebbleModule;

public class Issue3607 {
  @ServerTest
  public void shouldNotGenerateEmptyFlashMap(ServerTestRunner runner) throws InterruptedException {
    var latch = new CountDownLatch(1);
    var mustBeNull = new AtomicBoolean(false);
    runner
        .define(
            app -> {
              app.install(new PebbleModule());
              app.use(
                  next ->
                      ctx -> {
                        ctx.onComplete(
                            done -> {
                              mustBeNull.set(!done.getAttributes().containsKey(FlashMap.NAME));
                              latch.countDown();
                            });
                        return next.apply(ctx);
                      });
              app.get("/3607", ctx -> ModelAndView.map("index.pebble", Map.of("name", "Pebble")));
            })
        .ready(
            client -> {
              client.get(
                  "/3607",
                  rsp -> {
                    assertEquals("Hello Pebble!", rsp.body().string().trim());
                  });
            });
    latch.await();
    assertTrue(mustBeNull.get(), "Flash map must be null");
  }
}
