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
