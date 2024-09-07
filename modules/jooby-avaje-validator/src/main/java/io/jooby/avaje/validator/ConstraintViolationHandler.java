/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.validation.ConstraintViolation;
import io.avaje.validation.ConstraintViolationException;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;
import io.jooby.validation.ValidationResult;

/**
 * Catches and transform {@link ConstraintViolationException} into {@link ValidationResult}
 *
 * <p>Payload example:
 *
 * <pre>{@code
 * {
 *    "title": "Validation failed",
 *    "status": 422,
 *    "errors": [
 *       {
 *          "field": null,
 *          "messages": [
 *             "Passwords should match"
 *          ],
 *          "type": "GLOBAL"
 *       },
 *       {
 *          "field": "firstName",
 *          "messages": [
 *             "must not be empty",
 *             "must not be null"
 *          ],
 *          "type": "FIELD"
 *       }
 *    ]
 * }
 * }</pre>
 *
 * @author kliushnichenko
 * @since 3.2.10
 */
public class ConstraintViolationHandler implements ErrorHandler {
  private static final String ROOT_VIOLATIONS_PATH = "";
  private final StatusCode statusCode;
  private final String title;

  public ConstraintViolationHandler(@NonNull StatusCode statusCode, @NonNull String title) {
    this.statusCode = statusCode;
    this.title = title;
  }

  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    if (cause instanceof ConstraintViolationException ex) {
      var violations = ex.violations();

      var groupedByPath = violations.stream().collect(groupingBy(ConstraintViolation::path));
      var errors = collectErrors(groupedByPath);

      var result = new ValidationResult(title, statusCode.value(), errors);
      ctx.setResponseCode(statusCode).render(result);
    }
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
