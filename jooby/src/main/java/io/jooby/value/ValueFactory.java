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
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.SneakyThrows;
import io.jooby.exception.ProvisioningException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.reflect.$Types;

/**
 * Keep track of existing {@link Converter} and convert values to a more concrete type. This class
 * resolve all the <code>toXXX</code> calls from {@link Value}.
 *
 * <ul>
 *   <li>{@link Value#to(Class)}: convert to request type using {@link ConversionHint#Strict}
 *   <li>{@link Value#toNullable(Class)}: convert to request type using {@link
 *       ConversionHint#Nullable}
 *   <li>{@link io.jooby.QueryString#toEmpty(Class)}: convert to request type using {@link
 *       ConversionHint#Empty}
 * </ul>
 *
 * @author edgar
 * @since 4.0.0
 */
public class ValueFactory {

  private final Map<Type, Converter> converterMap = new HashMap<>();

  private ConversionHint defaultHint = ConversionHint.Strict;

  private MethodHandles.Lookup lookup;

  private Converter fallback;

  /**
   * Creates a new instance.
   *
   * @param lookup Lookup to use.
   */
  public ValueFactory(@NonNull MethodHandles.Lookup lookup) {
    this.lookup = lookup;
    this.fallback = new ReflectiveBeanConverter(lookup);
    StandardConverter.register(this);
  }

  /** Creates a new instance with public lookup. */
  public ValueFactory() {
    this(MethodHandles.publicLookup());
  }

  /**
   * Set lookup to use. Required by:
   *
   * <ul>
   *   <li>valueOf(String) converter
   *   <li>constructor(String) converter
   *   <li>fallback/reflective bean converter
   * </ul>
   *
   * @param lookup Look up to use.
   * @return This instance.
   */
  public @NonNull ValueFactory lookup(@NonNull MethodHandles.Lookup lookup) {
    this.lookup = lookup;
    this.fallback = new ReflectiveBeanConverter(lookup);
    return this;
  }

  /**
   * Set default conversion hint to use. Defaults is {@link ConversionHint#Strict}.
   *
   * @param defaultHint Default conversion hint.
   * @return This instance.
   */
  public @NonNull ValueFactory hint(@NonNull ConversionHint defaultHint) {
    this.defaultHint = defaultHint;
    return this;
  }

  /**
   * Get a converter for the given type. This is an exact lookup, no inheritance rule applies here.
   *
   * @param type The requested type.
   * @return A converter or <code>null</code>.
   */
  public @Nullable Converter get(Type type) {
    return converterMap.get(type);
  }

  /**
   * Set a custom converter for type.
   *
   * @param type Target type.
   * @param converter Converter.
   * @return This instance.
   */
  public @NonNull ValueFactory put(@NonNull Type type, @NonNull Converter converter) {
    converterMap.put(type, converter);
    return this;
  }

  /**
   * Convert a value to target type using the default {@link #hint(ConversionHint)}. Conversion
   * steps:
   *
   * <ul>
   *   <li>Find a converter by type and use it. If no converter is found:
   *   <li>Find a factory method <code>valueOf(String)</code> for {@link Value#isSingle()} values
   *       and use it. If no converter is found:
   *   <li>Find a <code>constructor(String)</code> for {@link Value#isSingle()} values. If no
   *       converter is found:
   *   <li>Fallback to reflective converter.
   * </ul>
   *
   * @param type Target type.
   * @param value Value.
   * @param <T> Target type.
   * @return New instance.
   * @throws TypeMismatchException when convert returns <code>null</code> and hint is set to {@link
   *     ConversionHint#Strict}.
   * @throws ProvisioningException when convert target type constructor requires a non-null value
   *     and value is missing or null.
   */
  public <T> T convert(@NonNull Type type, @NonNull Value value)
      throws TypeMismatchException, ProvisioningException {
    return convert(type, value, defaultHint);
  }

  /**
   * Convert a value to target type using a hint. Conversion steps:
   *
   * <ul>
   *   <li>Find a converter by type and use it. If no converter is found:
   *   <li>Find a factory method <code>valueOf(String)</code> for {@link Value#isSingle()} values
   *       and use it. If no converter is found:
   *   <li>Find a <code>constructor(String)</code> for {@link Value#isSingle()} values. If no
   *       converter is found:
   *   <li>Fallback to reflective converter.
   * </ul>
   *
   * @param type Target type.
   * @param value Value.
   * @param hint Conversion hint.
   * @param <T> Target type.
   * @return New instance.
   * @throws TypeMismatchException when convert returns <code>null</code> and hint is set to {@link
   *     ConversionHint#Strict}.
   * @throws ProvisioningException when convert target type constructor requires a non-null value
   *     and value is missing or null.
   */
  public <T> T convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint)
      throws TypeMismatchException, ProvisioningException {
    T result = convertInternal(type, value, hint);
    if (result == null && hint == ConversionHint.Strict) {
      throw new TypeMismatchException(value.name(), type);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private <T> T convertInternal(
      @NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
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
      return (T) fallback.convert(type, value, hint);
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
