package io.jooby.converter;

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
