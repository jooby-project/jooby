/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2858;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;

import io.jooby.Jooby;
import io.jooby.jetty.Jetty;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2858 {

  public static class App2858 extends Jooby {

    public static volatile boolean error = false;

    {
      ws(
          "/2858",
          (ctx, initializer) -> {
            initializer.onConnect(
                ws -> {
                  try {
                    ws.send(new JSONObject().put("connected", true).toString());
                  } catch (Exception x) {
                    x.printStackTrace();
                    error = true;
                  }
                });
            initializer.onMessage(
                (ws, message) -> {
                  ws.send(new JSONObject().put(message.value(), error).toString());
                });

            initializer.onError(
                (ws, cause) -> {
                  error = true;
                  getLog().error("websocket error ", cause);
                });
          });

      error(
          (ctx, cause, code) -> {
            error = true;
            getLog().error("application error ", cause);
          });
    }
  }

  @ServerTest(server = Jetty.class)
  public void shouldBeAbleToSendMessageOnConnect(ServerTestRunner runner) {
    App2858 app = new App2858();
    runner
        .use(
            () -> {
              app.error = false;
              return app;
            })
        .ready(
            client -> {
              client.syncWebSocket(
                  "/2858",
                  ws -> {
                    assertEquals("{\"connected\":true}", ws.lastMessage());
                    assertEquals("{\"error\":false}", ws.send("error"));
                  });
            });

    assertEquals(false, app.error);
  }
}
