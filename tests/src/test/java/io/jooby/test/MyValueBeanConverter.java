/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;
import io.jooby.value.Value;

public class MyValueBeanConverter implements Converter {

  @Override
  public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
    MyValue result = new MyValue();
    result.setString(value.get("string").value());
    return result;
  }
}
