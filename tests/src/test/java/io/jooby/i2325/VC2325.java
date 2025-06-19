/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2325;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import io.jooby.QueryString;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;
import io.jooby.value.Value;

public class VC2325 implements Converter {
  @Override
  public Object convert(@NotNull Type type, @NotNull Value value, @NotNull ConversionHint hint) {
    var v = value instanceof QueryString query ? query.get("value").value() : value.value();
    return new MyID2325(v);
  }
}
