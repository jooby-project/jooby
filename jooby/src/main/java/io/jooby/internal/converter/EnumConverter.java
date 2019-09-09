/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

public class EnumConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type.isEnum();
  }

  @Override public Enum convert(Class type, String value) {
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException x) {
      try {
        return Enum.valueOf(type, value.toUpperCase());
      } catch (IllegalArgumentException x1) {
        throw x;
      }
    }
  }
}
