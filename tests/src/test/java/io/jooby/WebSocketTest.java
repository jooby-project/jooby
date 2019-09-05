package io.jooby;

import io.jooby.netty.Netty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSocketTest {
  @Test
  public void webSocket() {
    new JoobyRunner(app -> {

      app.ws("/ws/{key}", ws -> {
//        ws.onMessage((ctx, value) -> {
//          ws.send("Hi " + value + "!");
//        });
      });

    }).ready(client -> {
      client.syncWebSocket("/ws/123", ws -> {
        assertEquals("Hi ws!", ws.send("ws"));
      });
    }, Netty::new);

  }
}
