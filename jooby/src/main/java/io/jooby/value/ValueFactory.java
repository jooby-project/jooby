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
import io.jooby.SneakyThrows;
import io.jooby.Value;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.converter.ReflectiveBeanConverter;
import io.jooby.internal.converter.StandardConverter;
import io.jooby.internal.reflect.$Types;

public class ValueFactory {

  public enum ConversionType {
    Strict,
    Nullable,
    Empty,
  }

  private final Map<Type, Converter> converterMap = new HashMap<>();

  private MethodHandles.Lookup lookup = MethodHandles.publicLookup();

  public ValueFactory() {
    StandardConverter.register(this);
  }

  public Converter get(Type type) {
    return converterMap.get(type);
  }

  public ValueFactory put(Type type, Converter converter) {
    converterMap.put(type, converter);
    return this;
  }

  public <T> T convert(@NonNull Type type, @NonNull Value value) {
    T result = convert(type, value, ConversionHint.Strict);
    if (result == null) {
      throw new TypeMismatchException(value.name(), type);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public <T> T convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
    var converter = converterMap.get(type);
    if (converter != null) {
      // Specific converter at type level.
      return (T) converter.convert(type, value, hint);
    }
    var rawType = $Types.getRawType(type);
    // Is it a container?
    if (List.class.isAssignableFrom(rawType)) {
      return (T) List.of(convert($Types.parameterizedType0(type), value));
    } else if (Set.class.isAssignableFrom(rawType)) {
      return (T) Set.of(convert($Types.parameterizedType0(type), value));
    } else if (Optional.class.isAssignableFrom(rawType)) {
      return (T) Optional.of(convert($Types.parameterizedType0(type), value));
    } else {
      // dynamic conversion
      if (Enum.class.isAssignableFrom(rawType)) {
        return (T) enumValue(value, (Class) rawType);
      }
      if (!value.isObject()) {
        // valueOf only works on non-object either a single or array[0]
        var valueOf = valueOf(rawType);
        if (valueOf != null) {
          try {
            return (T) valueOf.invoke(value.value());
          } catch (Throwable ex) {
            throw SneakyThrows.propagate(ex);
          }
        }
      }
      // anything else fallback to reflective
      var reflective = new ReflectiveBeanConverter();
      return (T) reflective.convert(value, rawType, hint == ConversionHint.Empty);
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
