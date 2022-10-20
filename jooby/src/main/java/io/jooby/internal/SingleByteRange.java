/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.BoundedInputStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

/**
 * Utility class to compute single byte range requests when response content length is known. Jooby
 * support single byte range requests on file responses, like: assets, input stream, files, etc.
 *
 * <p>Single byte range request looks like: <code>bytes=0-100</code>, <code>bytes=100-</code>,
 * <code>bytes=-100</code>.
 *
 * <p>Multiple byte range request are not supported.
 *
 * @since 2.0.0
 * @author edgar
 */
public class SingleByteRange implements ByteRange {
  private String value;

  private long start;

  private long end;

  private long contentLength;

  private String contentRange;

  public SingleByteRange(
      String value, long start, long end, long contentLength, String contentRange) {
    this.value = value;
    this.start = start;
    this.end = end;
    this.contentLength = contentLength;
    this.contentRange = contentRange;
  }

  /**
   * Start range or <code>-1</code>.
   *
   * @return Start range or <code>-1</code>.
   */
  @Override
  public long getStart() {
    return start;
  }

  /**
   * End range or <code>-1</code>.
   *
   * @return End range or <code>-1</code>.
   */
  @Override
  public long getEnd() {
    return end;
  }

  /**
   * New content length.
   *
   * @return New content length.
   */
  @Override
  public long getContentLength() {
    return contentLength;
  }

  /**
   * Value for <code>Content-Range</code> response header.
   *
   * @return Value for <code>Content-Range</code> response header.
   */
  @Override
  public @NonNull String getContentRange() {
    return contentRange;
  }

  @NonNull @Override
  public StatusCode getStatusCode() {
    return StatusCode.PARTIAL_CONTENT;
  }

  /**
   * For partial request this method set the following byte range response headers:
   *
   * <p>- Accept-Ranges - Content-Range - Content-Length
   *
   * <p>For not satisfiable requests:
   *
   * <p>- Throws a {@link StatusCode#REQUESTED_RANGE_NOT_SATISFIABLE}
   *
   * <p>Otherwise this method does nothing.
   *
   * @param ctx Web context.
   * @return This byte range request.
   */
  @Override
  public @NonNull ByteRange apply(@NonNull Context ctx) {
    ctx.setResponseHeader("Accept-Ranges", "bytes");
    ctx.setResponseHeader("Content-Range", contentRange);
    ctx.setResponseLength(contentLength);
    ctx.setResponseCode(StatusCode.PARTIAL_CONTENT);
    return this;
  }

  /**
   * For partial requests this method generates a new truncated input stream.
   *
   * <p>For not satisfiable requests this method throws an exception.
   *
   * <p>If there is no range to apply this method returns the given input stream.
   *
   * @param input Input stream.
   * @return A truncated input stream for partial request or same input stream.
   * @throws IOException When truncation fails.
   */
  @Override
  public @NonNull InputStream apply(@NonNull InputStream input) throws IOException {
    input.skip(start);
    return new BoundedInputStream(input, end);
  }

  @Override
  public String toString() {
    return value;
  }
}
