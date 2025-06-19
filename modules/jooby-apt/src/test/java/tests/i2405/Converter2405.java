/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2405;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;
import io.jooby.value.Value;

public class Converter2405 implements Converter {

  @Override
  public Object convert(@NotNull Type type, @NotNull Value value, @NonNull ConversionHint hint) {
    return new Bean2405(value.value());
  }
}
