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

public class ByteRange {

  private static final String BYTES_EQ = "bytes=";

  public static final ByteRange NO_RANGE = new ByteRange(-1, -1) {
    @Override public ByteRange apply(Context ctx, long contentLength) {
      return new ByteRange(0, contentLength);
    }
  };

  public static final ByteRange NOT_SATISFIABLE = new ByteRange(-1, -1) {
    @Override public ByteRange apply(Context ctx, long contentLength) {
      throw new Err(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
  };

  public final long start;

  public final long end;

  private ByteRange(long start, long end) {
    this.start = start;
    this.end = end;
  }

  public ByteRange apply(Context ctx, long contentLength) {
    long start = this.start;
    long end = this.end;
    if (start == -1) {
      start = contentLength - end;
      end = contentLength - 1;
    }
    if (end == -1 || end > contentLength - 1) {
      end = contentLength - 1;
    }
    if (start > end) {
      return NOT_SATISFIABLE;
    }
    // offset
    long limit = (end - start + 1);
    ctx.setHeader("Accept-Ranges", "bytes");
    ctx.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + contentLength);
    ctx.setHeader("Content-Length", limit);
    ctx.setStatusCode(StatusCode.PARTIAL_CONTENT);
    return new ByteRange(start, limit);
  }

  public boolean valid() {
    return this != NO_RANGE && this != NOT_SATISFIABLE;
  }

  public static ByteRange parse(String value) {
    if (value == null) {
      return NO_RANGE;
    }
    if (!value.startsWith(BYTES_EQ)) {
      return NOT_SATISFIABLE;
    }
    try {
      long[] range = {-1, -1};
      int r = 0;
      int len = value.length();
      int i = BYTES_EQ.length();
      int start = i;
      char ch;
      // Only Single Byte Range Requests:
      while (i < len && (ch = value.charAt(i)) != ',') {
        if (ch == '-') {
          if (start < i) {
            range[r] = Long.parseLong(value.substring(start, i).trim());
          }
          start = i + 1;
          r += 1;
        }
        i += 1;
      }
      if (start < i) {
        if (r == 0) {
          return NOT_SATISFIABLE;
        }
        range[r++] = Long.parseLong(value.substring(start, i).trim());
      }
      if (r == 0 || (range[0] == -1 && range[1] == -1)) {
        return NOT_SATISFIABLE;
      }
      return new ByteRange(range[0], range[1]);
    } catch (NumberFormatException x) {
      return NOT_SATISFIABLE;
    }
  }
}
