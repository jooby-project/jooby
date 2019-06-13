/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Thrown when a required service is not available.
 *
 */
public class RegistryException extends StatusCodeException {

  /**
   * Constructor.
   *
   * @param message Error message.
   * @param cause Cause.
   */
  public RegistryException(@Nonnull String message, Throwable cause) {
    super(StatusCode.SERVER_ERROR, message, cause);
  }

  /**
   * Constructor.
   *
   * @param message Error message.
   */
  public RegistryException(@Nonnull String message) {
    super(StatusCode.SERVER_ERROR, message);
  }
}
