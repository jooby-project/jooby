/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;

/**
 * Occurs when sensitive encoded data is set outside pac4j internals.
 *
 * @since 2.16.6
 */
public class Pac4jUntrustedDataFound extends StatusCodeException {
  public Pac4jUntrustedDataFound(String message) {
    super(StatusCode.FORBIDDEN, message);
  }
}
