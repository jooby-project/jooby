/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;
import org.apache.commons.io.input.BoundedInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to compute single byte range requests when response content length is known.
 * Jooby support single byte range requests on file responses, like: assets, input stream, files,
 * etc.
 *
 * Single byte range request looks like: <code>bytes=0-100</code>, <code>bytes=100-</code>,
 * <code>bytes=-100</code>.
 *
 * Multiple byte range request are not supported.
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

  public SingleByteRange(String value, long start, long end, long contentLength,
      String contentRange) {
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
  @Override public long getStart() {
    return start;
  }

  /**
   * End range or <code>-1</code>.
   *
   * @return End range or <code>-1</code>.
   */
  @Override public long getEnd() {
    return end;
  }

  /**
   * New content length.
   *
   * @return New content length.
   */
  @Override public long getContentLength() {
    return contentLength;
  }

  /**
   * Value for <code>Content-Range</code> response header.
   *
   * @return Value for <code>Content-Range</code> response header.
   */
  @Override public @Nonnull String getContentRange() {
    return contentRange;
  }

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.PARTIAL_CONTENT;
  }

  /**
   * For partial request this method set the following byte range response headers:
   *
   *  - Accept-Ranges
   *  - Content-Range
   *  - Content-Length
   *
   * For not satisfiable requests:
   *
   *  - Throws a {@link StatusCode#REQUESTED_RANGE_NOT_SATISFIABLE}
   *
   * Otherwise this method does nothing.
   *
   * @param ctx Web context.
   * @return This byte range request.
   */
  @Override public @Nonnull ByteRange apply(@Nonnull Context ctx) {
    ctx.setHeader("Accept-Ranges", "bytes");
    ctx.setHeader("Content-Range", contentRange);
    ctx.setContentLength(contentLength);
    ctx.setStatusCode(StatusCode.PARTIAL_CONTENT);
    return this;
  }

  /**
   * For partial requests this method generates a new truncated input stream.
   *
   * For not satisfiable requests this method throws an exception.
   *
   * If there is no range to apply this method returns the given input stream.
   *
   * @param input Input stream.
   * @return A truncated input stream for partial request or same input stream.
   * @throws IOException When truncation fails.
   */
  @Override public @Nonnull InputStream apply(@Nonnull InputStream input) throws IOException {
    input.skip(start);
    return new BoundedInputStream(input, end);
  }

  @Override public String toString() {
    return value;
  }

}
