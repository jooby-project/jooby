package io.jooby.exception;

import io.jooby.StatusCode;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Specific error for unauthorized access.
 *
 * @since 2.4.1
 * @author edgar
 */
public class UnauthorizedException extends StatusCodeException {
  /**
   * Creates an unauthorized exception.
   *
   * @param message Message. Optional.
   */
  public UnauthorizedException(@Nullable String message) {
    super(StatusCode.UNAUTHORIZED, Optional.ofNullable(message).orElse(""));
  }

  /**
   * Creates an unauthorized exception.
   */
  public UnauthorizedException() {
    this(null);
  }
}
