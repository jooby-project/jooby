/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.ws;

import io.jooby.Context;
import io.jooby.WebSocket;
import io.jooby.annotation.ws.OnMessage;

import java.util.Map;

public class WebsocketBeanMessage {

  public record Incoming(String text) {
  }

  @OnMessage
  public Map<String, String> onMessage(WebSocket ws, Context ctx, Incoming msg) {
    return Map.of("echo", msg.text());
  }
}
