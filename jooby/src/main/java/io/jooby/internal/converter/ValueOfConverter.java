/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ValueOfConverter extends FromStringConverter<Method> {

  @Override
  protected Object invoke(Method executable, String value)
      throws InvocationTargetException, IllegalAccessException {
    return executable.invoke(null, value);
  }

  @Override
  protected Method mappingMethod(Class<?> type) {
    try {
      Method valueOf = type.getDeclaredMethod("valueOf", String.class);
      if (Modifier.isStatic(valueOf.getModifiers()) && Modifier.isPublic(valueOf.getModifiers())) {
        return valueOf;
      }
      return null;
    } catch (NoSuchMethodException x) {
      return null;
    }
  }
}
