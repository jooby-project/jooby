/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2572;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2572 {

  @ServerTest
  public void onCompleteShouldRunOnCallerThread(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              Map<String, Object> state = new ConcurrentHashMap<>();

              CountDownLatch latch = new CountDownLatch(1);
              app.install(new JacksonModule());

              app.get(
                  "/2572/state",
                  ctx -> {
                    latch.await();
                    return state;
                  });

              app.use(
                  next ->
                      ctx -> {
                        state.put("caller", Thread.currentThread().getName());
                        ctx.onComplete(
                            context -> {
                              state.put("onComplete", Thread.currentThread().getName());
                              latch.countDown();
                            });
                        return next.apply(ctx);
                      });

              app.get("/2572/init", ctx -> "Initialized");
            })
        .ready(
            http -> {
              http.get(
                  "/2572/init",
                  rsp -> {
                    assertEquals("Initialized", rsp.body().string());
                  });

              http.get(
                  "/2572/state",
                  rsp -> {
                    JSONObject json = new JSONObject(rsp.body().string());
                    assertEquals(json.get("caller"), json.get("onComplete"));
                  });
            });
  }
}
