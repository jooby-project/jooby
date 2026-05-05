/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.avaje.validator;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.avaje.validation.ConstraintViolation;
import io.avaje.validation.ConstraintViolationException;
import io.jooby.StatusCode;
import io.jooby.validation.ValidationExceptionMapper;
import io.jooby.validation.ValidationResult;

public class ConstraintViolationMapper implements ValidationExceptionMapper {
  private static final String ROOT_VIOLATIONS_PATH = "";

  private final StatusCode statusCode;
  private final String title;

  public ConstraintViolationMapper(StatusCode statusCode, String title) {
    this.statusCode = statusCode;
    this.title = title;
  }

  @Override
  public ValidationResult toResult(StatusCode suggestedCode, Exception cause) {
    if (cause instanceof ConstraintViolationException constraintViolationException) {
      var violations = constraintViolationException.violations();

      var groupedByPath = violations.stream().collect(groupingBy(ConstraintViolation::path));
      var errors = collectErrors(groupedByPath);

      return new ValidationResult(title, statusCode.value(), errors);
    }
    return null;
  }

  private List<ValidationResult.Error> collectErrors(
      Map<String, List<ConstraintViolation>> groupedViolations) {
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

  private List<String> extractMessages(List<ConstraintViolation> violations) {
    return violations.stream().map(ConstraintViolation::message).toList();
  }
}
