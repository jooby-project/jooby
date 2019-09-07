package io.jooby.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.jooby.Value;

/**
 *  A restricted base type of {@link Value} for the {@link ValueConverter} SPI.
 * @author agentgt
 *
 */
public interface ValueContainer {

  /**
   * Get a value at the given position.
   *
   * @param index Position.
   * @return A value at the given position.
   */
  @Nonnull ValueContainer get(@Nonnull int index);

  /**
   * Get a value that matches the given name.
   *
   * @param name Field name.
   * @return Field value.
   */
  @Nonnull ValueContainer get(@Nonnull String name);

  /**
   * Get string value.
   *
   * @return String value.
   */
  @Nonnull String value();

  /**
   * Convert this value to String (if possible) or <code>null</code> when missing.
   *
   * @return Convert this value to String (if possible) or <code>null</code> when missing.
   */
  @Nullable String valueOrNull();

  /**
   * True for missing values.
   *
   * @return True for missing values.
   */
  boolean isMissing();

  /**
   * The number of values this one has. For single values size is <code>0</code>.
   *
   * @return Number of values. Mainly for array and hash values.
   */
  int size();

  /**
   * True if this value is an array/sequence (not single or hash).
   *
   * @return True if this value is an array/sequence.
   */
  boolean isArray();

  /**
   * True if this is a single value (not a hash or array).
   *
   * @return True if this is a single value (not a hash or array).
   */
  boolean isSingle();

  /**
   * True if this is a hash/object value (not single or array).
   *
   * @return True if this is a hash/object value (not single or array).
   */
  boolean isObject();

}
