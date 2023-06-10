/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.ValueConverters;

/**
 * Value converter for simple values that come from query, path, form, etc... parameters into more
 * specific type.
 *
 * <p>It is an extension point for {@link Value#to(Class)} calls.
 */
public interface ValueConverter<V extends Value> {
  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@NonNull Class type);

  /**
   * Convert simple to specific type.
   *
   * @param value Value value.
   * @param type Requested type.
   * @return Converted value.
   */
  Object convert(@NonNull V value, @NonNull Class type);

  /**
   * Immutable list of defaults/built-in {@link ValueConverter}.
   *
   * @return Immutable list of defaults/built-in {@link ValueConverter}.
   */
  static List<ValueConverter> defaults() {
    return ValueConverters.defaultConverters();
  }
}
