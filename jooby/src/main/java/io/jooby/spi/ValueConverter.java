package io.jooby.spi;

import javax.annotation.Nonnull;

/**
 * Value converter for string values that come from query, path, form parameters into more specific
 * type.
 */
public interface ValueConverter {
  /**
   * True if the converter applies for the given type.
   *
   * @param type Conversion type.
   * @return True if the converter applies for the given type.
   */
  boolean supports(@Nonnull Class type);

  /**
   * Convert string to specific type.
   *
   * @param type Requested type.
   * @param value String value.
   * @return Converted value.
   */
  Object convert(@Nonnull Class type, @Nonnull String value);
}
