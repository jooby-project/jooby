/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.StatusCode;

/**
 * Specific exception for bad request.
 *
 * @since 2.0.0
 * @author edgar
 */
public class BadRequestException extends StatusCodeException {

  /**
   * Creates a bad request exception.
   *
   * @param message Message.
   */
  public BadRequestException(@NonNull String message) {
    super(StatusCode.BAD_REQUEST, message);
  }

  /**
   * Creates a bad request exception.
   *
   * @param message Message.
   * @param cause Throwable.
   */
  public BadRequestException(@NonNull String message, @NonNull Throwable cause) {
    super(StatusCode.BAD_REQUEST, message, cause);
  }
}
