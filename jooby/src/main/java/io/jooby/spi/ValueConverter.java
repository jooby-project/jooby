package io.jooby.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.jooby.TypeMismatchException;
import io.jooby.Value;

public interface ValueConverter {
  /**
   * This defaults to true to allow a functional interface.
   * @param type
   * @return
   */
  default boolean supportsType(@Nonnull Class<?> type) {
    return true;
  }
  /**
   * Converts values for {@link Value#to} and friends. Returning null indicates the type is not supported
   * or the converter chose not to do the conversion.
   * @param value
   * @param type
   * @return <code>null</code> indicates that the converter chose to delegate to other converters down the chain.
   * @throws TypeMismatchException if the converter cannot convert the type and does not want to delegate.
   */
  @Nullable Object convert(@Nonnull ValueContainer value, @Nonnull Class<?> type) throws TypeMismatchException;
}
