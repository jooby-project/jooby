/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import javax.annotation.Nullable;

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
}
