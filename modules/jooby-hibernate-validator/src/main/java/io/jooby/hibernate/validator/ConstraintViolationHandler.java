/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import static io.jooby.validation.ValidationResult.ErrorType.FIELD;
import static io.jooby.validation.ValidationResult.ErrorType.GLOBAL;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;
import io.jooby.validation.ValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

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
 * @since 3.3.1
 */
public class ConstraintViolationHandler implements ErrorHandler {
  private static final String ROOT_VIOLATIONS_PATH = "";
  private final Logger log = LoggerFactory.getLogger(ConstraintViolationHandler.class);
  private final StatusCode statusCode;
  private final String title;
  private final boolean logException;
  private final boolean problemDetailsEnabled;

  public ConstraintViolationHandler(
      @NonNull StatusCode statusCode,
      @NonNull String title,
      boolean logException,
      boolean problemDetailsEnabled) {
    this.statusCode = statusCode;
    this.title = title;
    this.logException = logException;
    this.problemDetailsEnabled = problemDetailsEnabled;
  }

  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    if (cause instanceof ConstraintViolationException ex) {
      if (logException) {
        log.error(ErrorHandler.errorMessage(ctx, code), cause);
      }
      var violations = ex.getConstraintViolations();

      var groupedByPath =
          violations.stream()
              .collect(groupingBy(violation -> violation.getPropertyPath().toString()));

      var errors = collectErrors(groupedByPath);

      var result = new ValidationResult(title, statusCode.value(), errors);
      renderOrPropagate(ctx, result, code);
    }
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

  private void renderOrPropagate(Context ctx, ValidationResult result, StatusCode code) {
    if (problemDetailsEnabled) {
      ctx.getRouter().getErrorHandler().apply(ctx, result.toHttpProblem(), code);
    } else {
      ctx.setResponseCode(statusCode).render(result);
    }
  }
}
