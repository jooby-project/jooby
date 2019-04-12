package io.jooby;

public class ByteRange {
  private static final String BYTES_EQ = "bytes=";

  private String value;

  private long start;

  private long end;

  private long contentLength;

  private String contentRange;

  private StatusCode statusCode;

  private ByteRange(String value, long start, long end, long contentLength, String contentRange,
      StatusCode statusCode) {
    this.value = value;
    this.start = start;
    this.end = end;
    this.contentLength = contentLength;
    this.contentRange = contentRange;
    this.statusCode = statusCode;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public long getContentLength() {
    return contentLength;
  }

  public String getContentRange() {
    return contentRange;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  public boolean isPartial() {
    return statusCode == StatusCode.PARTIAL_CONTENT;
  }

  public ByteRange apply(Context ctx) {
    if (statusCode == StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE) {
      // Is throwing the right choice? Probably better to just send the status code and skip error
      throw new Err(statusCode, value);
    } else if (statusCode == StatusCode.PARTIAL_CONTENT) {
      ctx.setHeader("Accept-Ranges", "bytes");
      ctx.setHeader("Content-Range", contentRange);
      ctx.setContentLength(contentLength);
      ctx.setStatusCode(statusCode);
    }
    return this;
  }

  @Override public String toString() {
    return value;
  }

  public static ByteRange parse(String value, long contentLength) {
    if (contentLength <= 0 || value == null) {
      // NOOP
      return new ByteRange(value, 0, contentLength, contentLength, "bytes */" + contentLength,
          StatusCode.OK);
    }

    if (!value.startsWith(BYTES_EQ)) {
      return new ByteRange(value, 0, 0, contentLength, "bytes */" + contentLength,
          StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    try {
      long[] range = {-1, -1};
      int r = 0;
      int len = value.length();
      int i = BYTES_EQ.length();
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
          return new ByteRange(value, 0, 0, contentLength, "bytes */" + contentLength,
              StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
        }
        range[r++] = Long.parseLong(value.substring(offset, i).trim());
      }
      if (r == 0 || (range[0] == -1 && range[1] == -1)) {
        return new ByteRange(value, 0, 0, contentLength, "bytes */" + contentLength,
            StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
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
        return new ByteRange(value, 0, 0, contentLength, "bytes */" + contentLength,
            StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
      }
      // offset
      long limit = (end - start + 1);
      return new ByteRange(value, start, limit, limit,
          "bytes " + start + "-" + end + "/" + contentLength, StatusCode.PARTIAL_CONTENT);
    } catch (NumberFormatException expected) {
      return new ByteRange(value, 0, 0, contentLength, "bytes */" + contentLength,
          StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
  }
}
