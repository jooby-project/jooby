package io.jooby.converter;

import java.time.ZoneId;

public class ZoneIdConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == ZoneId.class;
  }

  @Override public Object convert(Class type, String value) {
    return ZoneId.of(value);
  }
}
