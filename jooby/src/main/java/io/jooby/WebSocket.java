package io.jooby;

public interface WebSocket {
  interface Handler {
    void apply(Context ctx);
  }

  interface OnConnect {
    void onConnect(WebSocket ws);
  }

  interface OnMessage {
    void onMessage(WebSocket ws);
  }

  void onConnect(OnConnect listener);

  void onMessage(OnMessage listener);

  void onError(WebSocket ws, Throwable cause, StatusCode statusCode);

  void onClose(WebSocket ws, StatusCode reason);
}
