/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import io.jooby.Value;
import io.jooby.ValueConverter;
import tests.i2325.MyID2325;

public class VC2525 implements ValueConverter {
  @Override
  public boolean supports(Class type) {
    return type == MyID2325.class;
  }

  @Override
  public Object convert(Value value, Class type) {
    return new MyID2325(value.value());
  }
}
