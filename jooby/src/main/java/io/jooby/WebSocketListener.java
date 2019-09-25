package io.jooby;

public interface WebSocketListener {

  WebSocketListener onConnect(WebSocket.OnConnect callback);

  WebSocketListener onMessage(WebSocket.OnMessage callback);

  WebSocketListener onError(WebSocket.OnError callback);

  WebSocketListener onClose(WebSocket.OnClose callback);
}
