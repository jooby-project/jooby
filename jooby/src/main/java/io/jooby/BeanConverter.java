/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Value converter for complex values that come from query, path, form, etc... parameters into more
 * specific type.
 *
 * It is an extension point for {@link ValueNode#to(Class)} calls.
 */
public interface BeanConverter {
  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@Nonnull Class type);

  /**
   * Convert a node value into more specific type.
   *
   * @param node Value value.
   * @param type Requested type.
   * @return Converted value.
   */
  Object convert(@Nonnull ValueNode node, @Nonnull Class type);
}
