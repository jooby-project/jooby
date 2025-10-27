/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import edu.umd.cs.findbugs.annotations.NonNull;

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
   * Detect or prevent duplicate route registrations. This option must be set before creating
   * routes.
   */
  private boolean failOnDuplicateRoutes = false;

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
   * When true handles X-Forwarded-* headers by updating the values on the current context to match
   * what was sent in the header(s).
   *
   * <p>This should only be installed behind a reverse proxy that has been configured to send the
   * <code>X-Forwarded-*</code> header, otherwise a remote user can spoof their address by sending a
   * header with bogus values.
   *
   * <p>The headers that are read/set are:
   *
   * <ul>
   *   <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.
   *   <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.
   *   <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.
   *   <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.
   * </ul>
   */
  private boolean trustProxy;

  /**
   * If enabled, allows to retrieve the {@link Context} object associated with the current request
   * via the service registry while the request is being processed.
   */
  private boolean contextAsService;

  /** Default constructor. */
  public RouterOptions() {}

  /**
   * If enabled, allows to retrieve the {@link Context} object associated with the current request
   * via the service registry while the request is being processed.
   *
   * @return If enabled, allows to retrieve the {@link Context} object associated with the current
   *     request via the service registry while the request is being processed.
   */
  public boolean isContextAsService() {
    return contextAsService;
  }

  /**
   * If enabled, allows to retrieve the {@link Context} object associated with the current request
   * via the service registry while the request is being processed.
   *
   * @param contextAsService True for enabled.
   * @return This options.
   */
  public RouterOptions setContextAsService(boolean contextAsService) {
    this.contextAsService = contextAsService;
    return this;
  }

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
   * Detect or prevent duplicate route registrations. This option must be set before creating
   * routes.
   *
   * @return Detect or prevent duplicate route registrations.
   */
  public boolean isFailOnDuplicateRoutes() {
    return failOnDuplicateRoutes;
  }

  /**
   * Detect or prevent duplicate route registrations.
   *
   * @param failOnDuplicateRoutes True for detect or prevent duplicate route registrations.
   * @return This options.
   */
  public RouterOptions setFailOnDuplicateRoutes(boolean failOnDuplicateRoutes) {
    this.failOnDuplicateRoutes = failOnDuplicateRoutes;
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

  /**
   * When true handles X-Forwarded-* headers by updating the values on the current context to match
   * what was sent in the header(s).
   *
   * <p>This should only be installed behind a reverse proxy that has been configured to send the
   * <code>X-Forwarded-*</code> header, otherwise a remote user can spoof their address by sending a
   * header with bogus values.
   *
   * <p>The headers that are read/set are:
   *
   * <ul>
   *   <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.
   *   <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.
   *   <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.
   *   <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.
   * </ul>
   *
   * @return True when enabled. Default is false.
   */
  public boolean isTrustProxy() {
    return trustProxy;
  }

  /**
   * When true handles X-Forwarded-* headers by updating the values on the current context to match
   * what was sent in the header(s).
   *
   * <p>This should only be installed behind a reverse proxy that has been configured to send the
   * <code>X-Forwarded-*</code> header, otherwise a remote user can spoof their address by sending a
   * header with bogus values.
   *
   * <p>The headers that are read/set are:
   *
   * <ul>
   *   <li>X-Forwarded-For: Set/update the remote address {@link Context#setRemoteAddress(String)}.
   *   <li>X-Forwarded-Proto: Set/update request scheme {@link Context#setScheme(String)}.
   *   <li>X-Forwarded-Host: Set/update the request host {@link Context#setHost(String)}.
   *   <li>X-Forwarded-Port: Set/update the request port {@link Context#setPort(int)}.
   * </ul>
   *
   * @param trustProxy True to enable.
   * @return This options.
   */
  @NonNull public RouterOptions setTrustProxy(boolean trustProxy) {
    this.trustProxy = trustProxy;
    return this;
  }
}
