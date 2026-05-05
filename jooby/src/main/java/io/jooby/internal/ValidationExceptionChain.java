/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import io.jooby.StatusCode;
import io.jooby.validation.ValidationExceptionMapper;
import io.jooby.validation.ValidationResult;

/**
 * ValidationExceptionChain provides a way to combine multiple {@link ValidationExceptionMapper}
 * implementations into a single chain. This allows sequential delegation of validation exception
 * mapping to the contained mappers.
 *
 * <p>The chain processes exceptions by iterating over the registered mappers. Each mapper attempts
 * to convert the given exception into a {@link ValidationResult}. The first non-null result found
 * is returned. If none of the mappers produce a result, a default {@link ValidationResult} is
 * generated with a global error indicating validation failure.
 *
 * <p>This class is useful in scenarios where different exception mapping strategies are needed and
 * should be applied in a specific sequence.
 *
 * @author edgar
 * @since 4.5.0
 */
public class ValidationExceptionChain implements ValidationExceptionMapper {
  private final List<ValidationExceptionMapper> mappers = new ArrayList<>();

  /**
   * Adds a {@link ValidationExceptionMapper} to the chain.
   *
   * <p>This method allows the registration of a new mapper, which will be used in sequence for
   * exception mapping. The newly added mapper will be appended to the chain, maintaining the order
   * of insertion.
   *
   * @param mapper the {@link ValidationExceptionMapper} to be added to the chain
   * @return the current {@link ValidationExceptionChain} instance to allow for method chaining
   */
  public ValidationExceptionChain add(ValidationExceptionMapper mapper) {
    mappers.add(mapper);
    return this;
  }

  /**
   * Converts the given {@link StatusCode} and {@link Exception} into a {@link ValidationResult}.
   *
   * <p>This method iterates through the chain of registered {@link ValidationExceptionMapper}
   * instances. Each mapper attempts to produce a {@link ValidationResult} for the specified status
   * code and exception. If a non-null result is produced, it is returned immediately. If no mapper
   * produces a valid result, a default {@link ValidationResult} is returned indicating a global
   * validation failure.
   *
   * @param suggestedCode the status code associated with the exception
   * @param cause the exception that needs to be converted into a validation result
   * @return the converted {@link ValidationResult} from the first applicable mapper, or a default
   *     result if no mapper can process the exception
   */
  @Override
  public @Nullable ValidationResult toResult(StatusCode suggestedCode, Exception cause) {
    for (var mapper : mappers) {
      var result = mapper.toResult(suggestedCode, cause);
      if (result != null) {
        return result;
      }
    }
    if (suggestedCode.value() >= 500) {
      // Not handled
      return null;
    }
    // Assume is a client error, provide a default result
    return new ValidationResult(
        "Validation failed",
        suggestedCode.value(),
        List.of(
            new ValidationResult.Error(
                null,
                List.of(
                    Optional.ofNullable(cause.getMessage())
                        .orElse(cause.getClass().getSimpleName())),
                ValidationResult.ErrorType.GLOBAL)));
  }
}
