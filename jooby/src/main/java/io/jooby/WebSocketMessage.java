package io.jooby;

import io.jooby.internal.WebSocketMessageImpl;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * Websocket message generated from a {@link WebSocket.OnMessage} callback. Message is a subclass.
 *
 * @author edgar
 * @since 2.2.0
 */
public interface WebSocketMessage extends Value {
  /**
   * Convert this value to the given type. Support values are single-value, array-value and
   * object-value. Object-value can be converted to a JavaBean type.
   *
   * @param type Type to convert.
   * @param <T> Element type.
   * @return Instance of the type.
   */
  @Nonnull <T> T to(@Nonnull Type type);

  /**
   * Creates a websocket message.
   *
   * @param ctx HTTP context.
   * @param bytes Text message as byte array.
   * @return A websocket message.
   */
  static @Nonnull WebSocketMessage create(@Nonnull Context ctx, @Nonnull byte[] bytes) {
    return new WebSocketMessageImpl(ctx, bytes);
  }
}
