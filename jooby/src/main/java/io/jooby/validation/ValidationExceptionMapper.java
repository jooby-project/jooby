/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import org.jspecify.annotations.Nullable;

import io.jooby.StatusCode;

/**
 * This interface defines a contract for mapping exceptions to validation results. It is primarily
 * used to convert exceptions, such as those thrown during bean validation, into instances of {@link
 * ValidationResult}. This allows for a consistent representation of validation errors across the
 * application.
 *
 * <p>Implementers are responsible for interpreting the given exception and translating it into an
 * appropriate {@link ValidationResult}, which may encapsulate details such as error messages,
 * status codes, and specific fields that failed validation.
 *
 * @author edgar
 * @since 4.5.0
 */
@FunctionalInterface
public interface ValidationExceptionMapper {

  /**
   * Converts the provided exception into a {@link ValidationResult}. This method interprets the
   * given exception, typically from a validation process, and maps it into a {@link
   * ValidationResult} instance, encapsulating details such as validation errors and status
   * information.
   *
   * @param suggestedCode the suggested status code for the validation result. Usually overriden
   *     with {@link StatusCode#UNPROCESSABLE_ENTITY}.
   * @param cause the exception to be mapped to a {@link ValidationResult}.
   * @return a {@link ValidationResult} representing the mapped exception.
   */
  @Nullable ValidationResult toResult(StatusCode suggestedCode, Exception cause);
}
