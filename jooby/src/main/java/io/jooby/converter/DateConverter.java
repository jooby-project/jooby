package io.jooby.converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Date.class;
  }

  @Override public Object convert(Class type, String value) {
    try {
      // must be millis
      return new Date(Long.parseLong(value));
    } catch (NumberFormatException x) {
      // must be YYYY-MM-dd
      LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
      return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
  }
}
