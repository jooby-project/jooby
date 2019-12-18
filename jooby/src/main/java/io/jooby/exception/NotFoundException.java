package io.jooby.exception;

import io.jooby.StatusCode;

import javax.annotation.Nonnull;

/**
 * When a request doesn't match any of the available routes.
 *
 * @since 2.4.0
 * @author edgar
 */
public class NotFoundException extends StatusCodeException {

  /**
   * Creates a not found exception.
   *
   * @param path Requested path.
   */
  public NotFoundException(@Nonnull String path) {
    super(StatusCode.NOT_FOUND, path);
  }

  /**
   * Requested path.
   *
   * @return Requested path.
   */
  public @Nonnull String getRequestPath() {
    return getMessage();
  }
}
