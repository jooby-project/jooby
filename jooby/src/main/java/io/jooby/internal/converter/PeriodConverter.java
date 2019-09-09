/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.time.Duration;
import java.time.Period;

public class PeriodConverter implements ValueConverter {
  private final DurationConverter converter = new DurationConverter();

  @Override public boolean supports(Class type) {
    return type == Period.class;
  }

  @Override public Object convert(Class type, String value) {
    return Period.from((Duration) converter.convert(type, value));
  }
}
