/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.problem.HttpProblem;

/**
 * Generate by CSRF handler.
 *
 * @author edgar
 * @since 2.5.2
 */
public class InvalidCsrfToken extends ForbiddenException {

  /**
   * Creates a new exception.
   *
   * @param token Token or <code>null</code>.
   */
  public InvalidCsrfToken(@Nullable String token) {
    super(token);
  }

  @Override
  public @NonNull HttpProblem toHttpProblem() {
    return HttpProblem.valueOf(
        statusCode, "Invalid CSRF token", "CSRF token '" + getMessage() + "' is invalid");
  }
}
