/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.Value;
import io.jooby.ValueConverter;

import java.math.BigDecimal;

public class BigDecimalConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == BigDecimal.class;
  }

  @Override public Object convert(Value value, Class type) {
    return new BigDecimal(value.value());
  }
}
