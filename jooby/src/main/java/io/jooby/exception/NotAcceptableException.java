/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;

/**
 * Whether the accept header isn't acceptable.
 *
 * @author edgar
 * @since 2.5.0
 */
public class NotAcceptableException extends StatusCodeException {
  /**
   * Creates a new exception.
   *
   * @param contentType Content-Type or <code>null</code>.
   */
  public NotAcceptableException(@Nullable String contentType) {
    super(StatusCode.NOT_ACCEPTABLE, contentType);
  }

  /**
   * Content-Type or <code>null</code>.
   *
   * @return Content-Type or <code>null</code>.
   */
  public @Nullable String getContentType() {
    return getMessage();
  }

  @Override
  public @NonNull HttpProblem toHttpProblem() {
    return HttpProblem.valueOf(
        statusCode,
        statusCode.reason(),
        "Server cannot produce a response matching the list of "
            + "acceptable values defined in the request's 'Accept' header");
  }
}
