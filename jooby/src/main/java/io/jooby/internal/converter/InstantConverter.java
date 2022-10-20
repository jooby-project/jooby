/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import io.jooby.Value;
import io.jooby.ValueConverter;

public class InstantConverter implements ValueConverter {
  @Override
  public boolean supports(Class type) {
    return type == Instant.class;
  }

  @Override
  public Object convert(Value value, Class type) {
    try {
      return Instant.ofEpochMilli(Long.parseLong(value.value()));
    } catch (NumberFormatException x) {
      return DateTimeFormatter.ISO_INSTANT.parse(value.value(), Instant::from);
    }
  }
}
