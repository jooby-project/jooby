package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.WebSocket;

import javax.annotation.Nonnull;

public class WebSocketHandler implements Route.Handler {
  private WebSocket.Handler handler;

  public WebSocketHandler(WebSocket.Handler handler) {
    this.handler = handler;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    boolean webSocket = ctx.header("Upgrade").value("").equalsIgnoreCase("WebSocket");
    if (webSocket) {
      return ctx.upgrade(handler);
    } else {
      return ctx.send(StatusCode.NOT_FOUND);
    }
  }
}
