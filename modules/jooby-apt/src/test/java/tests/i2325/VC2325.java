/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2325;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import io.jooby.Value;
import io.jooby.value.Converter;

public class VC2325 implements Converter {
  @Override
  public Object convert(@NotNull Type type, @NotNull Value value) {
    return new MyID2325(value.value());
  }
}
