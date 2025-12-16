/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3825;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3825 {
  @ServerTest
  public void shouldHaveAccessToBytes(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/3825",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> {
                          ws.send(
                              ">bytes: "
                                  + new String(message.bytes())
                                  + "; "
                                  + new String(message.byteBuffer().array()));
                        });
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/3825",
                  ws -> {
                    assertEquals(
                        ">bytes: bytes[]; bytes[]",
                        ws.sendBytes("bytes[]".getBytes(StandardCharsets.UTF_8)));
                  });
            });
  }
}
