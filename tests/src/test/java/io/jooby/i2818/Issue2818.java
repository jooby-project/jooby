/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2818;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2818 {
  @ServerTest
  public void shouldSendBinaryFromString(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/bin-text",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> ws.sendBinary("bin-text://" + message.value()));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/bin-text",
                  ws -> {
                    assertEquals("bin-text://binary", ws.send("binary"));
                  });
            });
  }

  @ServerTest
  public void shouldBroadcastBinaryFromString(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/bin-text",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> ws.sendBinary("bin-text://" + message.value(), true));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/bin-text",
                  ws -> {
                    assertEquals("bin-text://binary", ws.send("binary"));
                  });
            });
  }

  @ServerTest
  public void shouldSendBinaryFromByteArray(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/bin-text",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) ->
                            ws.sendBinary(
                                ("bin-bytes://" + message.value())
                                    .getBytes(StandardCharsets.UTF_8)));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/bin-text",
                  ws -> {
                    assertEquals("bin-bytes://binary", ws.send("binary"));
                  });
            });
  }

  @ServerTest
  public void shouldBroadcastBinaryFromByteArray(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/bin-text",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) ->
                            ws.sendBinary(
                                ("bin-bytes://" + message.value()).getBytes(StandardCharsets.UTF_8),
                                true));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/bin-text",
                  ws -> {
                    assertEquals("bin-bytes://binary", ws.send("binary"));
                  });
            });
  }

  @ServerTest
  public void shouldRenderBinary(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.ws(
                  "/ws/render-bin",
                  (ctx, initializer) -> {
                    initializer.onMessage((ws, message) -> ws.renderBinary(Map.of("foo", "bar")));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/render-bin",
                  ws -> {
                    assertEquals("{\"foo\":\"bar\"}", ws.send("binary"));
                  });
            });
  }

  @ServerTest
  public void shouldBroadcastRenderBinary(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.ws(
                  "/ws/render-bin",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> ws.renderBinary(Map.of("foo", "bar"), true));
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/render-bin",
                  ws -> {
                    assertEquals("{\"foo\":\"bar\"}", ws.send("binary"));
                  });
            });
  }
}
