/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2405;

import org.jetbrains.annotations.NotNull;

import io.jooby.Value;
import io.jooby.ValueConverter;

public class Converter2405 implements ValueConverter {
  @Override
  public boolean supports(@NotNull Class type) {
    return type == Bean2405.class;
  }

  @Override
  public Object convert(@NotNull Value value, @NotNull Class type) {
    return new Bean2405(value.value());
  }
}
