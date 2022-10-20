/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.util.UUID;

import io.jooby.Value;
import io.jooby.ValueConverter;

public class UUIDConverter implements ValueConverter {
  @Override
  public boolean supports(Class type) {
    return type == UUID.class;
  }

  @Override
  public Object convert(Value value, Class type) {
    return UUID.fromString(value.value());
  }
}
