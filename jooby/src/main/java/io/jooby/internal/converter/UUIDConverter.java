/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.Value;
import io.jooby.spi.ValueConverter;

import java.util.UUID;

public class UUIDConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == UUID.class;
  }

  @Override public UUID convert(Value value, Class type) {
    return UUID.fromString(value.value());
  }
}
