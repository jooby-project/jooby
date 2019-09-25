package io.jooby;

import com.fasterxml.jackson.databind.JsonNode;
import io.jooby.json.JacksonModule;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSocketTest {
  @Test
  public void webSocket() {
    new JoobyRunner(app -> {

      app.ws("/ws/{key}", (ctx, initializer) -> {
        initializer.onMessage((ws, message) -> {
          ws.send("Hi " + message.value() + "!");
        });
      });

    }).ready(client -> {
      client.syncWebSocket("/ws/123", ws -> {
        assertEquals("Hi ws!", ws.send("ws"));
      });
    }, Netty::new, Utow::new);

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
    }, Netty::new, Utow::new);

  }
}
