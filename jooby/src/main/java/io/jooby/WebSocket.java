/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.buffer.DataBuffer;

/**
 * Websocket. Usage:
 *
 * <pre>{@code
 * ws("/pattern", (ctx, configurer) -> {
 *   configurer.onConnect(ws -> {
 *     // Connect callback
 *   }):
 *
 *   configurer.onMessage((ws, message) -> {
 *     ws.send("Got: " + message.value());
 *   });
 *
 *   configurer.onClose((ws, closeStatus) -> {
 *     // Closing websocket
 *   });
 *
 *   configurer.onError((ws, cause) -> {
 *
 *   });
 * });
 *
 * }</pre>
 *
 * @author edgar
 * @since 2.2.0
 */
public interface WebSocket {
  /**
   * Websocket initializer. Give you access to a read-only {@link Context} you are free to access to
   * request attributes, while attempt to modify a response results in exception.
   */
  interface Initializer {
    /**
     * Callback with a readonly context and websocket configurer.
     *
     * @param ctx Readonly context.
     * @param configurer WebSocket configurer.
     */
    void init(@NonNull Context ctx, @NonNull WebSocketConfigurer configurer);
  }

  /** On connect callback. */
  interface OnConnect {
    /**
     * On connect callback with recently created web socket.
     *
     * @param ws WebSocket.
     */
    void onConnect(@NonNull WebSocket ws);
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
    void onMessage(@NonNull WebSocket ws, @NonNull WebSocketMessage message);
  }

  /**
   * On close callback. Generated when client close the connection or when explicit calls to {@link
   * #close(WebSocketCloseStatus)}.
   */
  interface OnClose {
    /**
     * Generated when client close the connection or when explicit calls to {@link
     * #close(WebSocketCloseStatus)}.
     *
     * @param ws WebSocket.
     * @param closeStatus Close status.
     */
    void onClose(@NonNull WebSocket ws, @NonNull WebSocketCloseStatus closeStatus);
  }

  /** On error callback. Generated when unexpected error occurs. */
  interface OnError {
    /**
     * Error callback, let you listen for exception. Websocket might or might not be open.
     *
     * @param ws Websocket.
     * @param cause Cause.
     */
    void onError(@NonNull WebSocket ws, @NonNull Throwable cause);
  }

  /** Callback for sending messages. */
  interface WriteCallback {

    WriteCallback NOOP = (ws, cause) -> {};

    /**
     * Notify about message sent to client.
     *
     * @param ws Websocket.
     * @param cause Error or <code>null</code> for success messages.
     */
    void operationComplete(@NonNull WebSocket ws, @Nullable Throwable cause);
  }

  /** Max message size for websocket (128K). */
  int MAX_BUFFER_SIZE = 131072;

  /**
   * Originating HTTP context. Please note this is a read-only context, so you are not allowed to
   * modify or produces a response from it.
   *
   * <p>The context let give you access to originating request (then one that was upgrade it).
   *
   * @return Read-only originating HTTP request.
   */
  @NonNull Context getContext();

  /**
   * Context attributes (a.k.a request attributes).
   *
   * @return Context attributes.
   */
  default @NonNull Map<String, Object> getAttributes() {
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
  default @NonNull <T> T attribute(@NonNull String key) {
    return getContext().getAttribute(key);
  }

  /**
   * Set an application attribute.
   *
   * @param key Attribute key.
   * @param value Attribute value.
   * @return This router.
   */
  default @NonNull WebSocket attribute(@NonNull String key, Object value) {
    getContext().setAttribute(key, value);
    return this;
  }

  /**
   * Web sockets connected to the same path. This method doesn't include the current websocket.
   *
   * @return Web sockets or empty list.
   */
  @NonNull List<WebSocket> getSessions();

  /**
   * True if websocket is open.
   *
   * @return True when open.
   */
  boolean isOpen();

  /**
   * For each of live sessions (including this) do something with it.
   *
   * <pre>{@code
   * Broadcast example:
   *
   * ws.forEach(session -> {
   *   session.send("Message");
   * });
   *
   * }</pre>
   *
   * @param callback Callback.
   */
  void forEach(SneakyThrows.Consumer<WebSocket> callback);

  /**
   * Send a text message to client.
   *
   * @param message Text Message.
   * @return This websocket.
   */
  default @NonNull WebSocket send(@NonNull String message) {
    return send(message, WriteCallback.NOOP);
  }

  /**
   * Send a text message to client.
   *
   * @param message Text Message.
   * @param callback Write callback.
   * @return This websocket.
   */
  @NonNull WebSocket send(@NonNull String message, @NonNull WriteCallback callback);

  /**
   * Send a text message to client.
   *
   * @param message Text Message.
   * @return This websocket.
   */
  default @NonNull WebSocket send(@NonNull byte[] message) {
    return send(message, WriteCallback.NOOP);
  }

  /**
   * Send a text message to client.
   *
   * @param message Text Message.
   * @param callback Write callback.
   * @return This websocket.
   */
  default @NonNull WebSocket send(@NonNull byte[] message, @NonNull WriteCallback callback) {
    return send(ByteBuffer.wrap(message), callback);
  }

  default @NonNull WebSocket send(@NonNull ByteBuffer message) {
    return send(message, WriteCallback.NOOP);
  }

  @NonNull WebSocket send(@NonNull ByteBuffer message, @NonNull WriteCallback callback);

  default @NonNull WebSocket send(@NonNull DataBuffer message) {
    return send(message, WriteCallback.NOOP);
  }

  @NonNull WebSocket send(@NonNull DataBuffer message, @NonNull WriteCallback callback);

  /**
   * Send a binary message to client.
   *
   * @param message Binary Message.
   * @return This websocket.
   */
  default @NonNull WebSocket sendBinary(@NonNull String message) {
    return sendBinary(message, WriteCallback.NOOP);
  }

  /**
   * Send a binary message to client.
   *
   * @param message Binary Message.
   * @param callback Write callback.
   * @return This websocket.
   */
  @NonNull WebSocket sendBinary(@NonNull String message, @NonNull WriteCallback callback);

  /**
   * Send a binary message to client.
   *
   * @param message Binary Message.
   * @return This websocket.
   */
  default @NonNull WebSocket sendBinary(@NonNull byte[] message) {
    return sendBinary(message, WriteCallback.NOOP);
  }

  /**
   * Send a binary message to client.
   *
   * @param message Binary Message.
   * @param callback Write callback.
   * @return This websocket.
   */
  default @NonNull WebSocket sendBinary(@NonNull byte[] message, @NonNull WriteCallback callback) {
    return sendBinary(ByteBuffer.wrap(message), callback);
  }

  default @NonNull WebSocket sendBinary(@NonNull ByteBuffer message) {
    return sendBinary(message, WriteCallback.NOOP);
  }

  @NonNull WebSocket sendBinary(@NonNull ByteBuffer message, @NonNull WriteCallback callback);

  default @NonNull WebSocket sendBinary(@NonNull DataBuffer message) {
    return sendBinary(message, WriteCallback.NOOP);
  }

  @NonNull WebSocket sendBinary(@NonNull DataBuffer message, @NonNull WriteCallback callback);

  /**
   * Encode a value and send a text message to client.
   *
   * @param value Value to send.
   * @return This websocket.
   */
  default @NonNull WebSocket render(@NonNull Object value) {
    return render(value, WriteCallback.NOOP);
  }

  /**
   * Encode a value and send a text message to client.
   *
   * @param value Value to send.
   * @param callback Write callback.
   * @return This websocket.
   */
  @NonNull WebSocket render(@NonNull Object value, @NonNull WriteCallback callback);

  /**
   * Encode a value and send a binary message to client.
   *
   * @param value Value to send.
   * @return This websocket.
   */
  default @NonNull WebSocket renderBinary(@NonNull Object value) {
    return renderBinary(value, WriteCallback.NOOP);
  }

  /**
   * Encode a value and send a binary message to client.
   *
   * @param value Value to send.
   * @param callback Write callback.
   * @return This websocket.
   */
  @NonNull WebSocket renderBinary(@NonNull Object value, @NonNull WriteCallback callback);

  /**
   * Close the web socket and send a {@link WebSocketCloseStatus#NORMAL} code to client.
   *
   * <p>This method fires a {@link OnClose#onClose(WebSocket, WebSocketCloseStatus)} callback.
   *
   * @return This websocket.
   */
  default @NonNull WebSocket close() {
    return close(WebSocketCloseStatus.NORMAL);
  }

  /**
   * Close the web socket and send a close status code to client.
   *
   * <p>This method fires a {@link OnClose#onClose(WebSocket, WebSocketCloseStatus)} callback.
   *
   * @param closeStatus Close status.
   * @return This websocket.
   */
  @NonNull WebSocket close(@NonNull WebSocketCloseStatus closeStatus);
}
