/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;
import io.jooby.problem.HttpProblemMappable;

/** Result of bean validator. Works a wrapper between validation results of external frameworks. */
public class ValidationResult implements HttpProblemMappable {

  private String title;
  private int status;
  private List<Error> errors;

  /** Default constructor. */
  public ValidationResult() {}

  /**
   * Creates a validation result.
   *
   * @param title Error title.
   * @param status Error status code.
   * @param errors Collection of offending checks.
   */
  public ValidationResult(String title, int status, List<Error> errors) {
    this.title = title;
    this.status = status;
    this.errors = errors;
  }

  @NonNull @Override
  public HttpProblem toHttpProblem() {
    return HttpProblem.builder()
        .title(title)
        .status(StatusCode.valueOf(status))
        .detail(errors.size() + " constraint violation(s) detected")
        .errors(convertErrors())
        .build();
  }

  private List<HttpProblem.Error> convertErrors() {
    List<HttpProblem.Error> problemErrors = new LinkedList<>();
    for (var err : errors) {
      for (var msg : err.messages()) {
        problemErrors.add(new HttpProblem.Error(msg, JsonPointer.of(err.field)));
      }
    }
    return problemErrors;
  }

  /**
   * Error message, common contract to render/serialize.
   *
   * @param field Field might be null.
   * @param messages Messages.
   * @param type Error type.
   */
  public record Error(
      @Nullable String field, @NonNull List<String> messages, @NonNull ErrorType type) {}

  /** Error type, describe when it is a generic/global error or specific/field error. */
  public enum ErrorType {
    /** Error is on field. */
    FIELD,
    /** Error isn't on specific field. */
    GLOBAL
  }

  /**
   * Result title.
   *
   * @return Result title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Result status code.
   *
   * @return Result status code.
   */
  public int getStatus() {
    return status;
  }

  /**
   * Offending errors.
   *
   * @return Offending errors.
   */
  public List<Error> getErrors() {
    return errors;
  }
}
