/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.ValueNode;
import io.jooby.ValueConverter;

import java.util.UUID;

public class UUIDConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == UUID.class;
  }

  @Override public UUID convert(ValueNode value, Class type) {
    return UUID.fromString(value.value());
  }
}
