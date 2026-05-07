/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;

/**
 * Exception thrown to indicate that a direct access attempt to resources via HTMX has been blocked.
 * This typically corresponds to an HTTP 406 Not Acceptable status.
 *
 * <p>HtmxDirectAccessException is a specialized form of {@code StatusCodeException} that sets.
 *
 * @author edgar
 * @since 4.5.0
 */
public class HtmxDirectAccessException extends StatusCodeException {
  /**
   * Constructs a new {@code HtmxDirectAccessException} with a default HTTP status code of 406 Not
   * Acceptable. This exception is used to signal that a direct access attempt to HTMX resources has
   * been disallowed.
   *
   * @param message The error message.
   */
  public HtmxDirectAccessException(String message) {
    super(StatusCode.NOT_ACCEPTABLE, message);
  }
}
