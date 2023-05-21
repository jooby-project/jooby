/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2828;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2828 {
  @ServerTest
  public void shouldAckWriteCallback(ServerTestRunner runner) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/write-callback",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) ->
                            ws.send(
                                message.value() + "-write-back",
                                (session, cause) -> {
                                  latch.countDown();
                                }));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/write-callback",
                  ws -> {
                    assertEquals("message-write-back", ws.send("message"));
                  });
            });
    latch.await(10, TimeUnit.SECONDS);
  }
}
