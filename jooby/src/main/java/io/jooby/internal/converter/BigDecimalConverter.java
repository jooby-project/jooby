/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.math.BigDecimal;

public class BigDecimalConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == BigDecimal.class;
  }

  @Override public BigDecimal convert(Class type, String value) {
    return new BigDecimal(value);
  }
}
