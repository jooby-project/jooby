/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import java.util.List;

public record ValidationResult(String title, int status, List<Error> errors) {
  public record Error(String field, List<String> messages, ErrorType type) {}

  public enum ErrorType {
    FIELD,
    GLOBAL
  }
}
