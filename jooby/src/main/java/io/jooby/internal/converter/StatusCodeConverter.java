package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import io.jooby.StatusCode;

public class StatusCodeConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == StatusCode.class;
  }

  @Override public StatusCode convert(Class type, String value) {
    return StatusCode.valueOf(Integer.parseInt(value));
  }
}
