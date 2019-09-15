/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Bean value converter, works like {@link ValueConverter} except that the input value is always
 * a hash/object value, not a simple string like it is required by {@link ValueConverter}.
 *
 * @since 2.1.1
 */
public interface BeanConverter {
  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@Nonnull Class type);

  Object convert(@Nonnull ValueNode node, @Nonnull Class type);
}
