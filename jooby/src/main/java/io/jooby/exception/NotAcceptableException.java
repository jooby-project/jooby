/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import io.jooby.StatusCode;

import javax.annotation.Nullable;

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
}
