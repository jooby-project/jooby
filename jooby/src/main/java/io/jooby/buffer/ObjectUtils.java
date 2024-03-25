/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

import java.util.List;

class ObjectUtils {

  public static boolean isEmpty(Object[] array) {
    return (array == null || array.length == 0);
  }

  public static boolean isEmpty(List<?> list) {
    return (list == null || list.isEmpty());
  }
}
