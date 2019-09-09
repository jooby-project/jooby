/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.time.Instant;

public class InstantConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == Instant.class;
  }

  @Override public Instant convert(Class type, String value) {
    return Instant.ofEpochMilli(Long.parseLong(value));
  }
}
