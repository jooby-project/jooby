/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Value;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;

public class MyValueBeanConverter implements Converter {

  @Override
  public Object convert(@NotNull Type type, @NotNull Value value, @NonNull ConversionHint hint) {
    MyValue result = new MyValue();
    result.setString(value.get("string").value());
    return result;
  }
}
