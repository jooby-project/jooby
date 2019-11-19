package io.jooby;

import com.fasterxml.jackson.databind.JsonNode;
import io.jooby.json.JacksonModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSocketTest {
  @Test
  public void webSocket() {
    new JoobyRunner(app -> {

      app.ws("/ws/{key}", (ctx, initializer) -> {
        StringBuilder buff = new StringBuilder(ctx.path("key").value());

        initializer.onConnect(ws -> {
          buff.append("/connected");
        });

        initializer.onMessage((ws, message) -> {
          ws.send(buff + "/" + message.value());
        });
      });

    }).ready(client -> {
      client.syncWebSocket("/ws/abc", ws -> {
        assertEquals("abc/connected/ws", ws.send("ws"));
      });
    });
  }

  @Test
  public void webSocketWithHttpSession() {
    new JoobyRunner(app -> {

      app.get("/create", ctx -> ctx.session().put("foo", "session").getId());

      app.ws("/session", (ctx, initializer) -> {
        StringBuilder buff = new StringBuilder(ctx.session().get("foo").value());

        initializer.onConnect(ws -> {
          buff.append("/connected");
        });

        initializer.onMessage((ws, message) -> {
          ws.send(buff.append("/").append(ctx.session().get("foo").value()).append("/")
              .append(message.value()).toString());
        });
      });

    }).ready(client -> {
      client.get("/create", rsp -> {
        String sid = sid(rsp.header("Set-Cookie"));
        client.header("Cookie", "jooby.sid=" + sid);
        client.syncWebSocket("/session", ws -> {
          assertEquals("session/connected/session/ws", ws.send("ws"));
        });
      });
    });
  }

  private String sid(String setCookie) {
    return setCookie.substring("jooby.sid=".length(), setCookie.indexOf(';'));
  }

  @Test
  public void webSocketJson() {
    new JoobyRunner(app -> {
      app.install(new JacksonModule());

      app.ws("/wsjson", (ctx, initializer) -> {
        initializer.onMessage((ws, message) -> {
          JsonNode node = message.to(JsonNode.class);
          ws.render(node);
        });
      });

    }).ready(client -> {
      client.syncWebSocket("/wsjson", ws -> {
        assertEquals("{\"message\":\"Hello JSON!\"}", ws.send("{\"message\" : \"Hello JSON!\"}"));
      });
    });
  }

}
