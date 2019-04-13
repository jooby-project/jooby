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
package io.jooby;

import io.jooby.internal.NoByteRange;
import io.jooby.internal.NotSatisfiableByteRange;
import io.jooby.internal.SingleByteRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
public interface ByteRange {
  /**
   * Byte range prefix.
   */
  String BYTES_RANGE = "bytes=";

  /**
   * Parse a byte range request value. Example of valid values:
   *
   * - bytes=0-100
   * - bytes=-100
   * - bytes=100-
   *
   * Any non-matching values produces a not satisfiable response.
   *
   * If value is null or content length less or equal to <code>0</code>, produces an empty/NOOP
   * response.
   *
   * @param value Valid byte range request value.
   * @param contentLength Content length.
   * @return Byte range instance.
   */
  static @Nonnull ByteRange parse(@Nullable String value, long contentLength) {
    if (contentLength <= 0 || value == null) {
      // NOOP
      return new NoByteRange(contentLength);
    }

    if (!value.startsWith(SingleByteRange.BYTES_RANGE)) {
      return new NotSatisfiableByteRange(value, contentLength);
    }

    try {
      long[] range = {-1, -1};
      int r = 0;
      int len = value.length();
      int i = SingleByteRange.BYTES_RANGE.length();
      int offset = i;
      char ch;
      // Only Single Byte Range Requests:
      while (i < len && (ch = value.charAt(i)) != ',') {
        if (ch == '-') {
          if (offset < i) {
            range[r] = Long.parseLong(value.substring(offset, i).trim());
          }
          offset = i + 1;
          r += 1;
        }
        i += 1;
      }
      if (offset < i) {
        if (r == 0) {
          return new NotSatisfiableByteRange(value, contentLength);
        }
        range[r++] = Long.parseLong(value.substring(offset, i).trim());
      }
      if (r == 0 || (range[0] == -1 && range[1] == -1)) {
        return new NotSatisfiableByteRange(value, contentLength);
      }

      long start = range[0];
      long end = range[1];
      if (start == -1) {
        start = contentLength - end;
        end = contentLength - 1;
      }
      if (end == -1 || end > contentLength - 1) {
        end = contentLength - 1;
      }
      if (start > end) {
        return new NotSatisfiableByteRange(value, contentLength);
      }
      // offset
      long limit = (end - start + 1);
      return new SingleByteRange(value, start, limit, limit,
          "bytes " + start + "-" + end + "/" + contentLength);
    } catch (NumberFormatException expected) {
      return new NotSatisfiableByteRange(value, contentLength);
    }
  }

  /**
   * Start range or <code>-1</code>.
   *
   * @return Start range or <code>-1</code>.
   */
  long getStart();

  /**
   * End range or <code>-1</code>.
   *
   * @return End range or <code>-1</code>.
   */
  long getEnd();

  /**
   * New content length.
   *
   * @return New content length.
   */
  long getContentLength();

  /**
   * Value for <code>Content-Range</code> response header.
   *
   * @return Value for <code>Content-Range</code> response header.
   */
  @Nonnull String getContentRange();

  /**
   * For partial requests this method returns {@link StatusCode#PARTIAL_CONTENT}.
   *
   * For not satisfiable requests this returns {@link StatusCode#REQUESTED_RANGE_NOT_SATISFIABLE}..
   *
   * Otherwise just returns {@link StatusCode#OK}.
   *
   * @return Status code.
   */
  @Nonnull StatusCode getStatusCode();

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
  @Nonnull ByteRange apply(@Nonnull Context ctx);

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
  @Nonnull InputStream apply(@Nonnull InputStream input) throws IOException;
}
