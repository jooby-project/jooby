package io.jooby;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Websocket. Usage:
 *
 * <pre>{@code
 *
 *     ws("/pattern", (ctx, configurer) -> {
 *       configurer.onConnect(ws -> {
 *         // Connect callback
 *       }):
 *
 *       configurer.onMessage((ws, message) -> {
 *         ws.send("Got: " + message.value());
 *       });
 *
 *       configurer.onClose((ws, closeStatus) -> {
 *         // Closing websocket
 *       });
 *
 *       configurer.onError((ws, cause) -> {
 *
 *       });
 *     });
 *
 * }</pre>
 *
 * @author edgar
 * @since 2.2.0
 */
public interface WebSocket {
  /**
   * Websocket initializer. Give you access to a read-only {@link Context} you are free to access
   * to request attributes, while attempt to modify a response results in exception.
   */
  interface Initializer {
    /**
     * Callback with a readonly context and websocket configurer.
     *
     * @param ctx Readonly context.
     * @param configurer WebSocket configurer.
     */
    void init(@Nonnull Context ctx, @Nonnull WebSocketConfigurer configurer);
  }

  /**
   * On connect callback.
   */
  interface OnConnect {
    /**
     * On connect callback with recently created web socket.
     *
     * @param ws WebSocket.
     */
    void onConnect(@Nonnull WebSocket ws);
  }

  /**
   * On message callback. When a Message is send by a client, this callback allow you to
   * handle/react to it.
   */
  interface OnMessage {
    /**
     * Generated when a client send a message.
     *
     * @param ws WebSocket.
     * @param message Client message.
     */
    void onMessage(@Nonnull WebSocket ws, @Nonnull WebSocketMessage message);
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
