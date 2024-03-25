/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.util.List;
import java.util.Objects;

class Assert {
  public static void notNull(Object charset, String message) {
    Objects.requireNonNull(charset, message);
  }

  public static void state(boolean value, String message) {
    if (!value) {
      throw new IllegalStateException(message);
    }
  }

  public static void isTrue(boolean value, String message) {
    if (!value) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void notEmpty(Object[] array, String message) {
    if (ObjectUtils.isEmpty(array)) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void notEmpty(List<?> list, String message) {
    if (ObjectUtils.isEmpty(list)) {
      throw new IllegalArgumentException(message);
    }
  }
}
