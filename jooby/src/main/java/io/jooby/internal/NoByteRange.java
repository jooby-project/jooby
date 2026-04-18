/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.IOException;
import java.io.InputStream;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

public class NoByteRange implements ByteRange {
  private long contentLength;

  public NoByteRange(long contentLength) {
    this.contentLength = contentLength;
  }

  @Override
  public long getStart() {
    return 0;
  }

  @Override
  public long getEnd() {
    return contentLength;
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  @Override
  public StatusCode getStatusCode() {
    return StatusCode.OK;
  }

  @Override
  public String getContentRange() {
    return "bytes */" + contentLength;
  }

  @Override
  public ByteRange apply(Context ctx) {
    return this;
  }

  @Override
  public InputStream apply(InputStream input) throws IOException {
    return input;
  }
}
