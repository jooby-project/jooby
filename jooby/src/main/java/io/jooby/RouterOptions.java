/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Router matching options. Specify whenever ignore case and trailing slash. Options:
 *
 * - ignoreCase: Indicates whenever routing algorithm does case-sensitive matching or not on
 *     incoming request path.
 *
 * - ignoreTrailingSlash: Indicates whenever a trailing slash is ignored or not on incoming request
 *     path.
 *
 * <pre>{@code
 *  {
 *    setRouterOptions(new RouterOptions()
 *        .setIgnoreCase(true)
 *    )
 *  }
 * }</pre>
 *
 * @author edgar
 * @since 2.0.0
 */
public class RouterOptions {
  private boolean ignoreCase;

  private boolean ignoreTrailingSlash;

  private boolean resetHeadersOnError = true;

  /**
   * Indicates whenever routing algorithm does case-sensitive matching or not on incoming request
   * path.
   *
   * This flag is on by default, so <code>/FOO</code> and <code>/foo</code> are not the same.
   *
   * @return Whenever do case-sensitive matching.
   */
  public boolean getIgnoreCase() {
    return ignoreCase;
  }

  /**
   * Turn on/off case-sensitive matching.
   *
   * @param ignoreCase True for ignore case while matching incoming request.
   * @return This options.
   */
  public RouterOptions setIgnoreCase(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
    return this;
  }

  /**
   * Indicates whenever a trailing slash is ignored or not on incoming request path.
   *
   * @return Whenever trailing slash on path pattern is ignored.
   */
  public boolean getIgnoreTrailingSlash() {
    return ignoreTrailingSlash;
  }

  /**
   * Turn on/off support for trailing slash.
   *
   * @param ignoreTrailingSlash True for turning on.
   * @return This options.
   */
  public RouterOptions setIgnoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.ignoreTrailingSlash = ignoreTrailingSlash;
    return this;
  }

  /**
   * True if response headers are cleared on application error. Reset is enabled by default.
   *
   * @return True if response headers are cleared on application error. Reset is enabled by default.
   */
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError;
  }

  /**
   * Set whenever reset/clear headers on application error.
   *
   * @param resetHeadersOnError True for reset/clear headers.
   * @return This context.
   */
  public @Nonnull RouterOptions setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }
}
