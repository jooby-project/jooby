/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.BeanConverter;
import io.jooby.ValueNode;

public class MyValueBeanConverter implements BeanConverter {
  @Override
  public boolean supports(@NonNull Class type) {
    return MyValue.class == type;
  }

  @Override
  public Object convert(@NonNull ValueNode value, @NonNull Class type) {
    MyValue result = new MyValue();
    result.setString(value.get("string").value());
    return result;
  }
}
