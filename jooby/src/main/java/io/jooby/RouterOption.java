package io.jooby;

public enum RouterOption {
  LOW_CASE,

  NO_TRAILING_SLASH,

  NORM,

  RESET_HEADERS_ON_ERROR;

  public int getMask() {
    return 1 << ordinal();
  }

  public boolean isEnabled(int flags) {
    return (flags & getMask()) != 0;
  }
}
