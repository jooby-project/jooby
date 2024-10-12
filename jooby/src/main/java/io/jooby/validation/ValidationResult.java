/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;
import io.jooby.problem.HttpProblemMappable;

import java.util.LinkedList;
import java.util.List;

public class ValidationResult implements HttpProblemMappable {

  private String title;
  private int status;
  private List<Error> errors;

  public ValidationResult(){}

  public ValidationResult(String title, int status, List<Error> errors) {
    this.title = title;
    this.status = status;
    this.errors = errors;
  }

  @NonNull
  @Override
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
    for (Error err : errors) {
      for (var msg : err.messages()) {
        problemErrors.add(new HttpProblem.Error(msg, JsonPointer.of(err.field)));
      }
    }
    return problemErrors;
  }

  public record Error(String field, List<String> messages, ErrorType type) {
  }

  public enum ErrorType {
    FIELD,
    GLOBAL
  }

  public String getTitle() {
    return title;
  }

  public int getStatus() {
    return status;
  }

  public List<Error> getErrors() {
    return errors;
  }
}
