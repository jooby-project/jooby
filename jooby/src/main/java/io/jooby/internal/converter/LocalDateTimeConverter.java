/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == LocalDateTime.class;
  }

  @Override public Object convert(Class type, String value) {
    try {
      // must be millis
      Instant instant = Instant.ofEpochMilli(Long.parseLong(value));
      return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    } catch (NumberFormatException x) {
      return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
  }
}
