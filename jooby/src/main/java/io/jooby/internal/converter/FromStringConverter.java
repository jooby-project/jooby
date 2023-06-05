/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import io.jooby.SneakyThrows;
import io.jooby.Value;
import io.jooby.ValueConverter;

public abstract class FromStringConverter<E extends Executable>
    implements ValueConverter, BiFunction<Class, E, E> {

  private Map<Class, E> cache = new ConcurrentHashMap<>();

  @Override
  public boolean supports(Class type) {
    return cache.compute(type, this) != null;
  }

  @Override
  public Object convert(Value value, Class type) {
    try {
      return invoke(cache.compute(type, this), value.value());
    } catch (InvocationTargetException x) {
      throw SneakyThrows.propagate(x.getTargetException());
    } catch (IllegalAccessException | InstantiationException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Override
  public E apply(Class type, E executable) {
    if (executable == null) {
      return mappingMethod(type);
    }
    return executable;
  }

  protected abstract Object invoke(E executable, String value)
      throws InvocationTargetException, IllegalAccessException, InstantiationException;

  protected abstract E mappingMethod(Class type);
}
