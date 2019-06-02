/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

public class RegistryException extends StatusCodeException {
  public RegistryException(@Nonnull String message, Throwable cause) {
    super(StatusCode.SERVER_ERROR, message, cause);
  }

  public RegistryException(@Nonnull String message) {
    super(StatusCode.SERVER_ERROR, message);
  }
}
