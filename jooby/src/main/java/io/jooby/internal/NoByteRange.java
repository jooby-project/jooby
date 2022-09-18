/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

import edu.umd.cs.findbugs.annotations.NonNull;
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

  @NonNull @Override public StatusCode getStatusCode() {
    return StatusCode.OK;
  }

  @NonNull @Override public String getContentRange() {
    return "bytes */" + contentLength;
  }

  @NonNull @Override public ByteRange apply(@NonNull Context ctx) {
    return this;
  }

  @NonNull @Override public InputStream apply(@NonNull InputStream input) throws IOException {
    return input;
  }
}
