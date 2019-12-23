/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Server-Sent message emitter.
 *
 * <pre>{@code
 *
 *   sse("/pattern", sse -> {
 *     sse.send("Hello Server-Sent events");
 *   })
 *
 * }</pre>
 *
 * @author edgar
 * @since 2.5.0
 */
public interface ServerSentEmitter {

  /**
   * Keep-alive task.
   */
  class KeepAlive implements Runnable {

    private final Logger log = LoggerFactory.getLogger(ServerSentEmitter.class);

    private ServerSentEmitter emitter;

    private long retry;

    public KeepAlive(final ServerSentEmitter emitter, final long retry) {
      this.emitter = emitter;
      this.retry = retry;
    }

    @Override
    public void run() {
      if (emitter.isOpen()) {
        String sseId = emitter.getId();
        try {
          log.debug("running heart beat for {}", sseId);
          emitter.send(":" + sseId + "\n");
          emitter.keepAlive(retry);
        } catch (Exception x) {
          log.debug("connection lost for {}", sseId, x);
          emitter.close();
        }
      }
    }
  }

  /**
   * Server-Sent event handler.
   */
  interface Handler {
    /**
     * Callback with a readonly context and sse configurer.
     *
     * @param sse Server sent event.
     * @throws Exception If something goes wrong.
     */
    void handle(@Nonnull ServerSentEmitter sse) throws Exception;
  }

  /**
   * Generated when client close the connection or when explicit calls to
   * {@link #close()}.
   *
   * @param task Cleanup task.
   */
  void onClose(SneakyThrows.Runnable task);

  /**
   * Originating HTTP context. Please note this is a read-only context, so you are not allowed
   * to modify or produces a response from it.
   *
   * The context let give you access to originating request (then one that was upgrade it).
   *
   * @return Read-only originating HTTP request.
   */
  @Nonnull Context getContext();

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
   * @return This ServerSent.
   */
  default @Nonnull ServerSentEmitter attribute(@Nonnull String key, Object value) {
    getContext().attribute(key, value);
    return this;
  }

  /**
   * Send a text message to client.
   *
   * @param data Text Message.
   * @return This ServerSent.
   */
  default @Nonnull ServerSentEmitter send(@Nonnull String data) {
    return send(new ServerSentMessage(data));
  }

  /**
   * Send a text message to client.
   *
   * @param data Text Message.
   * @return This ServerSent.
   */
  default @Nonnull ServerSentEmitter send(@Nonnull byte[] data) {
    return send(new ServerSentMessage(data));
  }

  /**
   * Send a text message to client.
   *
   * @param data Text Message.
   * @return This ServerSent.
   */
  default @Nonnull ServerSentEmitter send(@Nonnull Object data) {
    if (data instanceof ServerSentMessage) {
      return send((ServerSentMessage) data);
    } else {
      return send(new ServerSentMessage(data));
    }
  }

  /**
   * Send a message to the client and set the event type attribute.
   *
   * @param event Event type.
   * @param data Message.
   * @return This emitter.
   */
  default @Nonnull ServerSentEmitter send(@Nonnull String event, @Nonnull Object data) {
    return send(new ServerSentMessage(data).setEvent(event));
  }

  /**
   * Send a message to the client.
   *
   * @param data Message.
   * @return This emitter.
   */
  @Nonnull ServerSentEmitter send(@Nonnull ServerSentMessage data);

  /**
   * Send a comment message to the client. The comment line can be used to prevent connections
   * from timing out; a server can send a comment periodically to keep the connection alive.
   *
   * @param time Period of time.
   * @param unit Time unit.
   * @return This emitter.
   */
  default @Nonnull ServerSentEmitter keepAlive(final long time, final @Nonnull TimeUnit unit) {
    return keepAlive(unit.toMillis(time));
  }

  /**
   * Send a comment message to the client. The comment line can be used to prevent connections
   * from timing out; a server can send a comment periodically to keep the connection alive.
   *
   * @param timeInMillis Period of time in millis.
   * @return This emitter.
   */
  @Nonnull ServerSentEmitter keepAlive(long timeInMillis);

  /**
   * Read the <code>Last-Event-ID</code> header and retrieve it. Might be null.
   *
   * @return Last event ID or <code>null</code>.
   */
  default @Nullable String getLastEventId() {
    return lastEventId(String.class);
  }

  /**
   * Read the <code>Last-Event-ID</code> header and retrieve it. Might be null.
   *
   * @param type Type to convert.
   * @param <T> Type to convert.
   * @return Last event ID or <code>null</code>.
   */
  default @Nullable <T> T lastEventId(Class<T> type) {
    return getContext().header("Last-Event-ID").toOptional(type).orElse(null);
  }

  /**
   * Server-Sent ID. Defaults to UUID.
   *
   * @return Server-Sent ID. Defaults to UUID.
   */
  @Nonnull String getId();

  /**
   * Set Server-Sent ID.
   *
   * @param id Set Server-Sent ID.
   * @return This emitter.
   */
  @Nonnull ServerSentEmitter setId(@Nonnull String id);

  /**
   * True if connection is open.
   *
   * @return True if connection is open.
   */
  boolean isOpen();

  /**
   * Close the ServerSent and fir the close event.
   */
  void close();
}
