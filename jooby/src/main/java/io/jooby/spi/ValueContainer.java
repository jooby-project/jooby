package io.jooby.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  
  @Nonnull String value();
  
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
