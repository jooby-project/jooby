package io.jooby.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.jooby.TypeMismatchException;
import io.jooby.Value;

/**
 * An SPI for value conversion.
 * @author agentgt
 */
public interface BeanValueConverter {
  /**
   * A short circuit to see if the converter supports the given type.
   *
   * This defaults to true to allow a functional interface since convert can return null
   * to indicate it does not support the type.
   *
   * @param type class or interface
   * @return true if the converter can convert for the type
   */
  boolean supportsType(@Nonnull Class<?> type);
  /**
   * Converts values for {@link Value#to} and friends. Returning null indicates the type is not supported
   * or the converter chose not to do the conversion.
   * @param value the value to be converted
   * @param type the desired type. The type should be the equal or a super of the resulting instance returned.
   * @return <code>null</code> indicates that the converter chose to delegate to other converters down the chain.
   * @throws TypeMismatchException if the converter cannot convert the type and does not want to delegate.
   */
  @Nullable Object convert(@Nonnull Value value, @Nonnull Class<?> type) throws TypeMismatchException;
}
