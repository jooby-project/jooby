/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.ws;

import java.util.Map;

import io.jooby.Context;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.annotation.ws.OnClose;
import io.jooby.annotation.ws.OnConnect;
import io.jooby.annotation.ws.OnError;
import io.jooby.annotation.ws.OnMessage;
import io.jooby.annotation.ws.WebSocketRoute;

@WebSocketRoute("/chat/{username}")
public class ChatWebsocket {

  @OnConnect
  public String onConnect(WebSocket ws, Context ctx) {
    return "welcome";
  }

  @OnMessage
  public Map<String, String> onMessage(WebSocket ws, Context ctx, WebSocketMessage message) {
    return Map.of("echo", message.value());
  }

  @OnClose
  public void onClose(WebSocket ws, Context ctx, WebSocketCloseStatus status) {}

  @OnError
  public void onError(WebSocket ws, Context ctx, Throwable cause) {}
}
