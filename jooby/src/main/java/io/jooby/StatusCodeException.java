/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public StatusCodeException(@Nonnull StatusCode statusCode) {
    this(statusCode, statusCode.toString());
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   */
  public StatusCodeException(@Nonnull StatusCode statusCode, @Nonnull String message) {
    this(statusCode, message, null);
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   * @param cause Cause.
   */
  public StatusCodeException(@Nonnull StatusCode statusCode, @Nonnull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Status code.
   *
   * @return Status code.
   */
  public @Nonnull StatusCode getStatusCode() {
    return statusCode;
  }
}
