package org.jooby.internal.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Methods;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

import java.util.Optional;
import java.util.Set;

import org.jooby.WebSocket;
import org.jooby.WebSocket.Definition;
import org.jooby.internal.WebSocketImpl;

public class UndertowWebSocketHandler extends WebSocketProtocolHandshakeHandler {

  public static final AttachmentKey<WebSocketImpl> SOCKET =
      AttachmentKey.create(WebSocketImpl.class);

  private Set<Definition> sockets;

  private HttpHandler next;

  public UndertowWebSocketHandler(final WebSocketConnectionCallback callback,
      final HttpHandler next, final Set<WebSocket.Definition> sockets) {
    super(callback, next);
    this.next = next;
    this.sockets = sockets;
  }

  @Override
  public void handleRequest(final HttpServerExchange exchange) throws Exception {
    if (!exchange.getRequestMethod().equals(Methods.GET)) {
      // Only GET is supported to start the handshake
      next.handleRequest(exchange);
      return;
    }
    String path = exchange.getRequestPath();
    for (WebSocket.Definition socketDef : sockets) {
      Optional<WebSocket> matches = socketDef.matches(path);
      if (matches.isPresent()) {
        WebSocketImpl socket = (WebSocketImpl) matches.get();
        exchange.putAttachment(SOCKET, socket);
        super.handleRequest(exchange);
        return;
      }
    }
    // no luck, call next
    this.next.handleRequest(exchange);
  }

}
