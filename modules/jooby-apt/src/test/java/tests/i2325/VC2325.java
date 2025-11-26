/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2325;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.value.ConversionHint;
import io.jooby.value.Converter;
import io.jooby.value.Value;

public class VC2325 implements Converter {
  public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
    return new MyID2325(value.value());
  }
}
