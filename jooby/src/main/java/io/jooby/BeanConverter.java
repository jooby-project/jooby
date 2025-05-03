/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Value converter for complex values that come from query, path, form, etc... parameters into more
 * specific type.
 *
 * <p>It is an extension point for {@link ValueNode#to(Class)} calls.
 */
public interface BeanConverter extends ValueConverter<ValueNode> {

  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@NonNull Class<?> type);

  /**
   * Convert a node value into more specific type.
   *
   * @param node Value value.
   * @param type Requested type.
   * @return Converted value.
   */
  Object convert(@NonNull ValueNode node, @NonNull Class<?> type);
}
