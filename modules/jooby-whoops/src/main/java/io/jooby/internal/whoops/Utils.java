/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utils {
  public static Map<String, Object> multimap(Map<String, List<String>> input) {
    if (input.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> e : input.entrySet()) {
      if (e.getValue().size() == 1) {
        map.put(e.getKey(), e.getValue().get(0));
      } else {
        map.put(e.getKey(), e.getValue());
      }
    }
    return map;
  }

  public static Map<String, Object> mapOf(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      Object value = values[i + 1];
      if (!isEmpty(value)) {
        map.put(values[i].toString(), value);
      }
    }
    return map;
  }

  public static boolean isEmpty(Object value) {
    if (value instanceof Map) {
      return ((Map) value).isEmpty();
    }
    if (value instanceof Collection) {
      return ((Collection) value).isEmpty();
    }
    if (value == null) {
      return true;
    }
    return false;
  }
}
