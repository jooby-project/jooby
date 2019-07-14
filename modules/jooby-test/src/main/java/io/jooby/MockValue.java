/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Access to raw response value from {@link MockRouter} or cast response to something else.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface MockValue {
  /**
   * Raw response value.
   *
   * @return Raw response value.
   */
  @Nullable Object value();

  /**
   * Cast response to given type.
   *
   * @param type Type to cast.
   * @param <T> Response type.
   * @return Response value.
   */
  default @Nonnull <T> T value(@Nonnull Class<T> type) {
    Object instance = value();
    if (instance == null) {
      throw new ClassCastException("Found: null, expected: " + type);
    }
    if (!type.isInstance(instance)) {
      throw new ClassCastException("Found: " + instance.getClass() + ", expected: " + type);
    }
    return type.cast(instance);
  }
}
