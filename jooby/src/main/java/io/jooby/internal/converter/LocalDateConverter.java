/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == LocalDate.class;
  }

  @Override public Object convert(Class type, String value) {
    try {
      // must be millis
      Instant instant = Instant.ofEpochMilli(Long.parseLong(value));
      return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    } catch (NumberFormatException x) {
      // must be YYYY-MM-dd
      return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }
  }
}
