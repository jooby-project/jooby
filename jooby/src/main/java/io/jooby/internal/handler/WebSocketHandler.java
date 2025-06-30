/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.WebSocket;

public class WebSocketHandler implements WebSocket.Handler {
  private WebSocket.Initializer handler;

  public WebSocketHandler(WebSocket.Initializer handler) {
    this.handler = handler;
  }

  public WebSocket.Initializer getInitializer() {
    return handler;
  }

  @NonNull @Override
  public Object apply(@NonNull Context ctx) {
    boolean webSocket = ctx.header("Upgrade").value("").equalsIgnoreCase("WebSocket");
    if (webSocket) {
      ctx.upgrade(handler);
    }
    if (!ctx.isResponseStarted()) {
      return ctx.send(StatusCode.NOT_FOUND);
    }
    return ctx;
  }
}
