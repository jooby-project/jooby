/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import io.jooby.StatusCode;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Runtime exception with status code.
 *
 * @author edgar
 * @since 2.0.0
 */
public class StatusCodeException extends RuntimeException {

  private final StatusCode statusCode;

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   */
  public StatusCodeException(@NonNull StatusCode statusCode) {
    this(statusCode, statusCode.toString());
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   */
  public StatusCodeException(@NonNull StatusCode statusCode, @NonNull String message) {
    this(statusCode, message, null);
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   * @param cause Cause.
   */
  public StatusCodeException(@NonNull StatusCode statusCode, @NonNull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Status code.
   *
   * @return Status code.
   */
  public @NonNull StatusCode getStatusCode() {
    return statusCode;
  }
}
