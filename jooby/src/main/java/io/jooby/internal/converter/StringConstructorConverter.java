/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class StringConstructorConverter extends FromStringConverter<Constructor<?>> {
  @Override
  protected Object invoke(Constructor<?> executable, String value)
      throws InvocationTargetException, IllegalAccessException, InstantiationException {
    return executable.newInstance(value);
  }

  @Override
  protected Constructor<?> mappingMethod(Class<?> type) {
    try {
      return type.getDeclaredConstructor(String.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
