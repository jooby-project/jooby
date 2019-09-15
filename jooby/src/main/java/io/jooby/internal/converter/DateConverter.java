/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.ValueNode;
import io.jooby.ValueConverter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Date.class;
  }

  @Override public Object convert(ValueNode value, Class type) {
    try {
      // must be millis
      return new Date(Long.parseLong(value.value()));
    } catch (NumberFormatException x) {
      // must be YYYY-MM-dd
      LocalDate date = LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE);
      return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
  }
}
