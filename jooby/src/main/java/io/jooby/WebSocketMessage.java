package io.jooby;

import io.jooby.internal.WebSocketMessageImpl;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

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

  static WebSocketMessage create(Context ctx, byte[] bytes) {
    return new WebSocketMessageImpl(ctx, bytes);
  }
}
