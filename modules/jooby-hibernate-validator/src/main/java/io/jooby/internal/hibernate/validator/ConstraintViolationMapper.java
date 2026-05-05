/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import io.jooby.StatusCode;
import io.jooby.validation.ValidationExceptionMapper;
import io.jooby.validation.ValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

public class ConstraintViolationMapper implements ValidationExceptionMapper {
  private static final String ROOT_VIOLATIONS_PATH = "";

  private final String title;

  private final StatusCode statusCode;

  public ConstraintViolationMapper(StatusCode statusCode, String title) {
    this.statusCode = statusCode;
    this.title = title;
  }

  @Override
  public @Nullable ValidationResult toResult(StatusCode suggestedCode, Exception cause) {
    if (cause instanceof ConstraintViolationException constraintViolation) {
      var violations = constraintViolation.getConstraintViolations();
      var groupedByPath =
          violations.stream()
              .collect(groupingBy(violation -> violation.getPropertyPath().toString()));

      var errors = collectErrors(groupedByPath);

      return new ValidationResult(title, statusCode.value(), errors);
    }
    return null;
  }

  private List<ValidationResult.Error> collectErrors(
      Map<String, List<ConstraintViolation<?>>> groupedViolations) {
    List<ValidationResult.Error> errors = new ArrayList<>();
    for (var entry : groupedViolations.entrySet()) {
      var path = entry.getKey();
      if (ROOT_VIOLATIONS_PATH.equals(path)) {
        errors.add(new ValidationResult.Error(null, extractMessages(entry.getValue()), GLOBAL));
      } else {
        errors.add(new ValidationResult.Error(path, extractMessages(entry.getValue()), FIELD));
      }
    }
    return errors;
  }

  private List<String> extractMessages(List<ConstraintViolation<?>> violations) {
    return violations.stream().map(ConstraintViolation::getMessage).toList();
  }
}
