package io.jooby.exception;

import io.jooby.StatusCode;

import javax.annotation.Nullable;

/**
 *
 */
public class NotAcceptableException extends StatusCodeException {
  public NotAcceptableException(@Nullable String contentType) {
    super(StatusCode.NOT_ACCEPTABLE, contentType);
  }

  public String getContentType() {
    return getMessage();
  }
}
