/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

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
  default @NonNull <T> T value(@NonNull Class<T> type) {
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
