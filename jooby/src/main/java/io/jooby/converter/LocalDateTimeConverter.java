package io.jooby.converter;

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
