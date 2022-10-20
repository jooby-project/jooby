/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.time.ZoneId;

import io.jooby.Value;
import io.jooby.ValueConverter;

public class ZoneIdConverter implements ValueConverter {
  @Override
  public boolean supports(Class type) {
    return type == ZoneId.class;
  }

  @Override
  public Object convert(Value value, Class type) {
    return ZoneId.of(value.value());
  }
}
