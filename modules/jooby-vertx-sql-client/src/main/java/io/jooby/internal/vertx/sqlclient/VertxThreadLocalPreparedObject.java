/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx.sqlclient;

import java.util.List;
import java.util.Map;

public class VertxThreadLocalPreparedObject {

  private static final ThreadLocal<Map<String, List<Object>>> locals = new ThreadLocal<>();

  @SuppressWarnings({"rawtypes", "unchecked"})
  static void set(String name, List preparedList) {
    var m = locals.get();
    if (m == null) {
      locals.set(Map.of(name, preparedList));
    } else {
      var entries = new Map.Entry[m.size() + 1];
      m.entrySet().toArray(entries);
      entries[entries.length - 1] = Map.entry(name, preparedList);
      locals.set(Map.ofEntries(entries));
    }
  }

  @SuppressWarnings("unchecked")
  static <T> List<T> get(String name) {
    var m = locals.get();
    if (m == null) {
      return null;
    }
    return (List<T>) m.get(name);
  }
}
