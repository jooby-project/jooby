package io.jooby;

public interface WebSocketContext {
  void onConnect(WebSocket.OnConnect callback);

  void onMessage(WebSocket.OnMessage callback);

  void onError(WebSocket.OnError callback);

  void onClose(WebSocket.OnClose callback);
}
