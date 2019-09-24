package io.jooby;

public interface WebSocket {
  interface Handler {
    void apply(WebSocketContext ctx);
  }

  interface OnConnect {
    void onConnect(WebSocket ws);
  }

  interface OnMessage {
    void onMessage(WebSocket ws, WebSocketMessage message);
  }

  interface OnClose {
    void onClose(WebSocket ws, WebSocketCloseStatus closeStatus);
  }

  interface OnError {
    void onError(WebSocket ws, Throwable cause);
  }

  Context getContext();

  WebSocket send(String message);

  WebSocket send(byte[] bytes);

  WebSocket render(Object message);
}
