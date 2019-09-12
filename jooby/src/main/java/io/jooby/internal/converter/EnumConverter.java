/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.Value;
import io.jooby.spi.ValueConverter;

import java.util.EnumSet;
import java.util.Set;

public class EnumConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type.isEnum();
  }

  @Override public Enum convert(Value value, Class type) {
    try {
      return Enum.valueOf(type, value.value().toUpperCase());
    } catch (IllegalArgumentException x) {
      String name = value.value();
      // Fallback to ignore case version:
      Set<Enum> enums = EnumSet.allOf(type);
      for (Enum e : enums) {
        if (e.name().equalsIgnoreCase(name)) {
          return e;
        }
      }
      throw x;
    }
  }
}
