package io.jooby.converter;

import java.time.Instant;

public class InstantConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Instant.class;
  }

  @Override public Instant convert(Class type, String value) {
    return Instant.ofEpochMilli(Long.parseLong(value));
  }
}
