package io.jooby;

public class RouterOptions {
  private boolean caseSensitive = true;

  private boolean ignoreTrailingSlash = true;

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public RouterOptions setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isIgnoreTrailingSlash() {
    return ignoreTrailingSlash;
  }

  public RouterOptions setIgnoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.ignoreTrailingSlash = ignoreTrailingSlash;
    return this;
  }
}
