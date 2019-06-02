/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class NoByteRange implements ByteRange {
  private long contentLength;

  public NoByteRange(long contentLength) {
    this.contentLength = contentLength;
  }

  @Override public long getStart() {
    return 0;
  }

  @Override public long getEnd() {
    return contentLength;
  }

  @Override public long getContentLength() {
    return contentLength;
  }

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.OK;
  }

  @Nonnull @Override public String getContentRange() {
    return "bytes */" + contentLength;
  }

  @Nonnull @Override public ByteRange apply(@Nonnull Context ctx) {
    return this;
  }

  @Nonnull @Override public InputStream apply(@Nonnull InputStream input) throws IOException {
    return input;
  }
}
