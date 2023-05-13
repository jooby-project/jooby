/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class WebSocketTest {
  @ServerTest
  public void webSocket(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/{key}",
                  (ctx, initializer) -> {
                    StringBuilder buff = new StringBuilder(ctx.path("key").value());

                    initializer.onConnect(
                        ws -> {
                          buff.append("/connected");
                        });

                    initializer.onMessage(
                        (ws, message) -> {
                          ws.send(buff + "/" + message.value());
                        });
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/abc",
                  ws -> {
                    assertEquals("abc/connected/ws", ws.send("ws"));
                  });
            });
  }

  @ServerTest
  public void webSocketRecieveByteMessage(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.ws(
                  "/ws/{key}",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> {
                          ws.send(">" + message.value());
                        });
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/ws/abc",
                  ws -> {
                    assertEquals(
                        ">bytes[]", ws.sendBytes("bytes[]".getBytes(StandardCharsets.UTF_8)));
                  });
            });
  }

  @ServerTest
  public void webSocketWithHttpSession(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.get("/create", ctx -> ctx.session().put("foo", "session").getId());

              app.ws(
                  "/session",
                  (ctx, initializer) -> {
                    StringBuilder buff = new StringBuilder(ctx.session().get("foo").value());

                    initializer.onConnect(
                        ws -> {
                          buff.append("/connected");
                        });

                    initializer.onMessage(
                        (ws, message) -> {
                          ws.send(
                              buff.append("/")
                                  .append(ctx.session().get("foo").value())
                                  .append("/")
                                  .append(message.value())
                                  .toString());
                        });
                  });
            })
        .ready(
            client -> {
              client.get(
                  "/create",
                  rsp -> {
                    String sid = sid(rsp.header("Set-Cookie"));
                    client.header("Cookie", "jooby.sid=" + sid);
                    client.syncWebSocket(
                        "/session",
                        ws -> {
                          assertEquals("session/connected/session/ws", ws.send("ws"));
                        });
                  });
            });
  }

  @ServerTest
  public void webSocketJson(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.ws(
                  "/wsjson",
                  (ctx, initializer) -> {
                    initializer.onMessage(
                        (ws, message) -> {
                          JsonNode node = message.to(JsonNode.class);
                          ws.render(node);
                        });
                  });
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/wsjson",
                  ws -> {
                    assertEquals(
                        "{\"message\":\"Hello JSON!\"}",
                        ws.send("{\"message\" : \"Hello JSON!\"}"));
                  });
            });
  }

  @ServerTest
  public void webSocketBinary(ServerTestRunner runner) {
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

  private String sid(String setCookie) {
    return setCookie.substring("jooby.sid=".length(), setCookie.indexOf(';'));
  }
}
