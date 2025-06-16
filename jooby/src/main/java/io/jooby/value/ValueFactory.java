/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import java.lang.reflect.Type;
import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Value;
import io.jooby.internal.converter.BuiltinConverter;
import io.jooby.internal.reflect.$Types;

public class ValueFactory {

  private final Map<Class<?>, Converter> converterMap = new HashMap<>();

  public ValueFactory() {
    for (var converter : BuiltinConverter.values()) {
      converter.register(this);
    }
  }

  public <T> Converter get(Class<T> type) {
    return converterMap.get(type);
  }

  public <T> ValueFactory put(Class<T> type, Converter converter) {
    converterMap.put(type, converter);
    return this;
  }

  public Object convert(@NonNull Type type, @NonNull Value value) {
    if (type instanceof Class<?> clazz) {
      var converter = get(clazz);
      return converter.convert(clazz, value);
    } else {
      var rawType = $Types.getRawType(type);
      if (List.class.isAssignableFrom(rawType)) {
        return List.of(convert($Types.parameterizedType0(type), value));
      } else if (Set.class.isAssignableFrom(rawType)) {
        return Set.of(convert($Types.parameterizedType0(type), value));
      } else if (Optional.class.isAssignableFrom(rawType)) {
        return Optional.of(convert($Types.parameterizedType0(type), value));
      }
      throw new UnsupportedOperationException("Unsupported type: " + type);
    }
  }
}
