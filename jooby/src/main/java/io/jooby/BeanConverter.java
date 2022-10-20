/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.converter.ReflectiveBeanConverter;

/**
 * Value converter for complex values that come from query, path, form, etc... parameters into more
 * specific type.
 *
 * <p>It is an extension point for {@link ValueNode#to(Class)} calls.
 */
public interface BeanConverter {
  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@NonNull Class type);

  /**
   * Convert a node value into more specific type.
   *
   * @param node Value value.
   * @param type Requested type.
   * @return Converted value.
   */
  Object convert(@NonNull ValueNode node, @NonNull Class type);

  /**
   * Creates a bean converter that uses reflection.
   *
   * @return Reflection bean converter.
   */
  static BeanConverter reflective() {
    return new ReflectiveBeanConverter();
  }

  static void addFallbackConverters(List<BeanConverter> input) {
    input.add(reflective());
  }
}
