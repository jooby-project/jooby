package io.jooby;

import javax.annotation.Nonnull;
import java.util.Map;

public interface WebSocket {
  interface Handler {
    void init(Context ctx, WebSocketListener initializer);
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

  /**
   * Context attributes (a.k.a request attributes).
   *
   * @return Context attributes.
   */
  default @Nonnull Map<String, Object> getAttributes() {
    return getContext().getAttributes();
  }

  /**
   * Get an attribute by his key. This is just an utility method around {@link #getAttributes()}.
   * This method look first in current context and fallback to application attributes.
   *
   * @param key Attribute key.
   * @param <T> Attribute type.
   * @return Attribute value.
   */
  default @Nonnull <T> T attribute(@Nonnull String key) {
    return getContext().attribute(key);
  }

  /**
   * Set an application attribute.
   *
   * @param key Attribute key.
   * @param value Attribute value.
   * @return This router.
   */
  default @Nonnull WebSocket attribute(@Nonnull String key, Object value) {
    getContext().attribute(key, value);
    return this;
  }

  default WebSocket send(String message) {
    return send(message, false);
  }

  WebSocket send(String message, boolean broadcast);

  default WebSocket send(byte[] bytes) {
    return send(bytes, false);
  }

  WebSocket send(byte[] bytes, boolean broadcast);

  default WebSocket render(Object message) {
    return render(message, false);
  }

  WebSocket render(Object message, boolean broadcast);

}
