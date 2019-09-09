/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;
import io.jooby.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class ValueOfConverter implements ValueConverter, BiFunction<Class, Method, Method> {

  private Map<Class, Method> cache = new ConcurrentHashMap<>();

  @Override public boolean supports(Class type) {
    return cache.compute(type, this) != null;
  }

  @Override public Object convert(Class type, String value) {
    try {
      return cache.compute(type, this).invoke(null, value);
    } catch (InvocationTargetException x) {
      throw SneakyThrows.propagate(x.getTargetException());
    } catch (IllegalAccessException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override public Method apply(Class type, Method method) {
    if (method == null) {
      try {
        Method valueOf = type.getDeclaredMethod("valueOf", String.class);
        if (Modifier.isStatic(valueOf.getModifiers()) && Modifier
            .isPublic(valueOf.getModifiers())) {
          return valueOf;
        }
        return null;
      } catch (NoSuchMethodException x) {
        return null;
      }
    }
    return method;
  }
}
