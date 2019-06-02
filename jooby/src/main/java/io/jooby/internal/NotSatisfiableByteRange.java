/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCodeException;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class NotSatisfiableByteRange implements ByteRange {
  private String value;
  private long contentLength;

  public NotSatisfiableByteRange(String value, long contentLength) {
    this.value = value;
    this.contentLength = contentLength;
  }

  @Override public long getStart() {
    return -1;
  }

  @Override public long getEnd() {
    return -1;
  }

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE;
  }

  @Override public long getContentLength() {
    return contentLength;
  }

  @Nonnull @Override public String getContentRange() {
    return "bytes */" + contentLength;
  }

  @Nonnull @Override public ByteRange apply(@Nonnull Context ctx) {
    throw new StatusCodeException(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, value);
  }

  @Nonnull @Override public InputStream apply(@Nonnull InputStream input) throws IOException {
    throw new StatusCodeException(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, value);
  }
}
