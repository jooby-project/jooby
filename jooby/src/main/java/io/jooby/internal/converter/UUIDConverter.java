package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.util.UUID;

public class UUIDConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == UUID.class;
  }

  @Override public UUID convert(Class type, String value) {
    return UUID.fromString(value);
  }
}
