package io.jooby;

import io.jooby.internal.LimitedInputStream;

import java.io.InputStream;

public class Functions {
  public static InputStream limit(InputStream in, long limit) {
    return new LimitedInputStream(in, limit);
  }
}
