/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

public class Http2Settings {
  private final int maxRequestSize;
  private final boolean secure;

  public Http2Settings(long maxRequestSize, boolean secure) {
    this.maxRequestSize = (int) maxRequestSize;
    this.secure = secure;
  }

  public boolean isSecure() {
    return secure;
  }

  public int getMaxRequestSize() {
    return maxRequestSize;
  }
}
