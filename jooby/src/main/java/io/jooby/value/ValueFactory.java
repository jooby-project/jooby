/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.QueryString;
import io.jooby.SneakyThrows;
import io.jooby.Value;
import io.jooby.ValueNode;
import io.jooby.internal.converter.BuiltinConverter;
import io.jooby.internal.converter.ReflectiveBeanConverter;
import io.jooby.internal.converter.StandardConverter;
import io.jooby.internal.reflect.$Types;

public class ValueFactory {

  private final Map<Type, Converter> converterMap = new HashMap<>();

  private MethodHandles.Lookup lookup = MethodHandles.publicLookup();

  private MethodType methodType = MethodType.methodType(Object.class, String.class);

  public ValueFactory() {
    StandardConverter.register(this);
    BuiltinConverter.register(this);
  }

  public Converter get(Type type) {
    return converterMap.get(type);
  }

  public ValueFactory put(Type type, Converter converter) {
    converterMap.put(type, converter);
    return this;
  }

  public Object convert(@NonNull Type type, @NonNull Value value) {
    var converter = converterMap.get(type);
    if (converter != null) {
      // Specific converter at type level.
      return converter.convert(type, value);
    }
    var rawType = $Types.getRawType(type);
    // Is it a container?
    if (List.class.isAssignableFrom(rawType)) {
      return List.of(convert($Types.parameterizedType0(type), value));
    } else if (Set.class.isAssignableFrom(rawType)) {
      return Set.of(convert($Types.parameterizedType0(type), value));
    } else if (Optional.class.isAssignableFrom(rawType)) {
      return Optional.of(convert($Types.parameterizedType0(type), value));
    } else {
      // dynamic conversion
      if (Enum.class.isAssignableFrom(rawType)) {
        return enumValue(value, (Class) rawType);
      }
      if (!value.isObject()) {
        // valueOf only works on non-object either a single or array[0]
        var valueOf = valueOf(rawType);
        if (valueOf != null) {
          try {
            return valueOf.invoke(value.value());
          } catch (Throwable ex) {
            throw SneakyThrows.propagate(ex);
          }
        }
      }
      // anything else fallback to reflective
      var reflective = new ReflectiveBeanConverter();
      // TODO: review emptyBean flag
      return reflective.convert((ValueNode) value, rawType, value instanceof QueryString);
    }
  }

  private MethodHandle valueOf(Class<?> rawType) {
    try {
      // Factory method first
      return lookup.findStatic(rawType, "valueOf", MethodType.methodType(rawType, String.class));
    } catch (NoSuchMethodException ignored) {
      try {
        // Fallback to constructor
        return lookup.findConstructor(rawType, MethodType.methodType(void.class, String.class));
      } catch (NoSuchMethodException inner) {
        return null;
      } catch (IllegalAccessException inner) {
        throw SneakyThrows.propagate(inner);
      }
    } catch (IllegalAccessException cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  private static <T extends Enum<T>> Object enumValue(Value node, Class<T> type) {
    var name = node.value();
    try {
      return Enum.valueOf(type, name.toUpperCase());
    } catch (IllegalArgumentException x) {
      for (var e : EnumSet.allOf(type)) {
        if (e.name().equalsIgnoreCase(name)) {
          return e;
        }
      }
      throw x;
    }
  }
}
