/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NameGenerator {
  Map<String, AtomicInteger> registry = new HashMap<>();

  public boolean has(String... names) {
    return registry.containsKey(key(names));
  }

  public String generate(String... names) {
    String name = key(names);
    int c = registry.computeIfAbsent(name, k -> new AtomicInteger(-1)).incrementAndGet();
    return c == 0 ? name : name + '$' + c;
  }

  private String key(String[] names) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(names[0]);
    for (int i = 1; i < names.length; i++) {
      buffer.append(Character.toUpperCase(names[i].charAt(0))).append(names[i].substring(1));
    }
    return buffer.toString();
  }
}
