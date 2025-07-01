/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * Router options:
 *
 * <ul>
 *   <li>ignoreCase: Indicates whenever routing algorithm does case-sensitive matching on an
 *       incoming request path. Default is <code> false</code> (case sensitive).
 *   <li>ignoreTrailingSlash: Indicates whenever a trailing slash is ignored on an incoming request
 *       path.
 *   <li>normalizeSlash: Normalize an incoming request path by removing consecutive <code>/</code>
 *       (slashes).
 *   <li>resetHeadersOnError: Indicates whenever response headers are clear/reset in case of
 *       exception.
 * </ul>
 *
 * @author edgar
 * @since 2.4.0
 */
public class RouterOptions {
  /**
   * Indicates whenever routing algorithm does case-sensitive matching on an incoming request path.
   * Default is <code>case sensitive</code>.
   */
  private boolean ignoreCase;

  /** Indicates whenever a trailing slash is ignored on an incoming request path. */
  private boolean ignoreTrailingSlash;

  /** Normalize an incoming request path by removing multiple slash sequences. */
  private boolean normalizeSlash;

  /** Indicates whenever response headers are clear/reset in case of exception. */
  private boolean resetHeadersOnError;

  /**
   * Indicates whenever routing algorithm does case-sensitive matching on an incoming request path.
   * Default is <code>false (case sensitive)</code>.
   *
   * @return True when case is ignored.
   */
  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  /**
   * Indicates whenever routing algorithm does case-sensitive matching on an incoming request path.
   * Default is <code>false (case sensitive)</code>.
   *
   * @param ignoreCase True for case-insensitive.
   * @return This options.
   */
  public RouterOptions setIgnoreCase(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
    return this;
  }

  /**
   * Indicates whenever routing algorithm does case-sensitive matching on an incoming request path.
   * Default is <code>false (case sensitive)</code>.
   *
   * @param ignoreCase True for case-insensitive.
   * @return This options.
   */
  public RouterOptions ignoreCase(boolean ignoreCase) {
    return setIgnoreCase(ignoreCase);
  }

  /**
   * Indicates whenever a trailing slash is ignored on an incoming request path.
   *
   * @return Indicates whenever a trailing slash is ignored on an incoming request path.
   */
  public boolean isIgnoreTrailingSlash() {
    return ignoreTrailingSlash;
  }

  /**
   * Set whenever a trailing slash is ignored on an incoming request path.
   *
   * @param ignoreTrailingSlash whenever a trailing slash is ignored on an incoming request path.
   * @return This options.
   */
  public RouterOptions setIgnoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.ignoreTrailingSlash = ignoreTrailingSlash;
    return this;
  }

  /**
   * Set whenever a trailing slash is ignored on an incoming request path.
   *
   * @param ignoreTrailingSlash whenever a trailing slash is ignored on an incoming request path.
   * @return This options.
   */
  public RouterOptions ignoreTrailingSlash(boolean ignoreTrailingSlash) {
    return setIgnoreTrailingSlash(ignoreTrailingSlash);
  }

  /**
   * Normalize an incoming request path by removing multiple slash sequences.
   *
   * @return Normalize an incoming request path by removing multiple slash sequences.
   */
  public boolean isNormalizeSlash() {
    return normalizeSlash;
  }

  /**
   * Set whenever normalize an incoming request path by removing multiple slash sequences.
   *
   * @param normalizeSlash True for normalize a path.
   * @return This options.
   */
  public RouterOptions setNormalizeSlash(boolean normalizeSlash) {
    this.normalizeSlash = normalizeSlash;
    return this;
  }

  /**
   * Set whenever normalize an incoming request path by removing multiple slash sequences.
   *
   * @param normalizeSlash True for normalize a path.
   * @return This options.
   */
  public RouterOptions normalizeSlash(boolean normalizeSlash) {
    return setNormalizeSlash(normalizeSlash);
  }

  /**
   * Indicates whenever response headers are clear/reset in case of exception.
   *
   * @return Indicates whenever response headers are clear/reset in case of exception.
   */
  public boolean isResetHeadersOnError() {
    return resetHeadersOnError;
  }

  /**
   * Set whenever response headers are clear/reset in case of exception.
   *
   * @param resetHeadersOnError whenever response headers are clear/reset in case of exception.
   * @return This options.
   */
  public RouterOptions setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }

  /**
   * Set whenever response headers are clear/reset in case of exception.
   *
   * @param resetHeadersOnError whenever response headers are clear/reset in case of exception.
   * @return This options.
   */
  public RouterOptions resetHeaderOnError(boolean resetHeadersOnError) {
    return setResetHeadersOnError(resetHeadersOnError);
  }

  /**
   * Case-sensitive with reset headers on error enabled.
   *
   * @return Default options.
   */
  public static RouterOptions defaults() {
    return new RouterOptions().resetHeaderOnError(true);
  }

  /**
   * Case-inSensitive with reset headers on error enabled.
   *
   * @return Default options.
   */
  public static RouterOptions caseInsensitive() {
    return new RouterOptions().ignoreCase(true).resetHeaderOnError(true);
  }
}
