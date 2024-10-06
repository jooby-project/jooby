/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;

/**
 * Whether there is no decoder for the requested <code>Content-Type</code>.
 *
 * @since 2.4.1
 * @author edgar
 */
public class UnsupportedMediaType extends StatusCodeException {
  /**
   * Unsupported media type.
   *
   * @param type Content Type. Optional.
   */
  public UnsupportedMediaType(@Nullable String type) {
    super(StatusCode.UNSUPPORTED_MEDIA_TYPE, type);
  }

  /**
   * Content type.
   *
   * @return Content type.
   */
  public @Nullable String getContentType() {
    return getMessage();
  }

  @Override
  public @NonNull HttpProblem toHttpProblem() {
    return HttpProblem.valueOf(statusCode,
        statusCode.reason(),
        "Media type '" + getContentType() + "' is not supported");
  }
}
