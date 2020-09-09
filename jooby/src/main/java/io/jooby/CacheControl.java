/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.time.Duration;
import java.util.function.Function;

/**
 * Class allowing the fine tune the browser cache behavior for assets.
 *
 * @see AssetHandler
 * @see AssetHandler#cacheControl(Function)
 */
public class CacheControl {

  /**
   * Constant for the max-age parameter, when set, no {@code Cache-Control} header is generated.
   *
   * @see #setMaxAge(long)
   */
  public static final int UNDEFINED = -1;

  /**
   * Constant for the max-age parameter, when set, the {@code Cache-Control} header is set to {@code no-store, must-revalidate}.
   *
   * @see #setMaxAge(long)
   */
  public static final int NO_CACHE = -2;

  private boolean etag = true;
  private boolean lastModified = true;
  private long maxAge = -1;

  /**
   * Returns whether e-tag support is enabled.
   *
   * @return {@code true} if enabled.
   */
  public boolean isEtag() {
    return etag;
  }

  /**
   * Returns whether the handling of {@code If-Modified-Since} header is enabled.
   *
   * @return {@code true} if enabled.
   */
  public boolean isLastModified() {
    return lastModified;
  }

  /**
   * Returns the max-age header parameter value.
   *
   * @return the max-age header parameter value.
   */
  public long getMaxAge() {
    return maxAge;
  }

  /**
   * Turn on/off e-tag support.
   *
   * @param etag True for turning on.
   * @return This instance.
   */
  public CacheControl setETag(boolean etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Turn on/off handling of {@code If-Modified-Since} header.
   *
   * @param lastModified True for turning on. Default is: true.
   * @return This instance.
   */
  public CacheControl setLastModified(boolean lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  /**
   * Set cache-control header with the given max-age value. If max-age is greater than 0.
   *
   * @param maxAge Max-age value in seconds.
   * @return This instance.
   * @see #UNDEFINED
   * @see #NO_CACHE
   */
  public CacheControl setMaxAge(long maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  /**
   * Set cache-control header with the given max-age value. If max-age is greater than 0.
   *
   * @param maxAge Max-age value in seconds.
   * @return This instance.
   */
  public CacheControl setMaxAge(Duration maxAge) {
    this.maxAge = maxAge.getSeconds();
    return this;
  }

  /**
   * Set cache-control header to {@code no-store, must-revalidate}, disables e-tag
   * and {@code If-Modified-Since} header support.
   *
   * @return This instance.
   */
  public CacheControl setNoCache() {
    this.etag = false;
    this.lastModified = false;
    this.maxAge = NO_CACHE;
    return this;
  }

  /**
   * Returns the default caching configuration for assets.
   * <ul>
   *   <li>e-tag support: enabled</li>
   *   <li>{@code If-Modified-Since} support: enabled</li>
   *   <li>max-age: {@link #UNDEFINED} (no {@code Cache-Control} header is generated)</li>
   * </ul>
   *
   * @return the default cache configuration.
   */
  public static CacheControl defaults() {
    return new CacheControl();
  }

  /**
   * Returns a caching configuration for disabling cache completely.
   * <ul>
   *   <li>e-tag support: disabled</li>
   *   <li>{@code If-Modified-Since} support: disabled</li>
   *   <li>max-age: {@link #NO_CACHE} (the {@code Cache-Control} header is set to {@code no-store, must-revalidate})</li>
   * </ul>
   *
   * @return the default cache configuration.
   */
  public static CacheControl noCache() {
    return defaults().setNoCache();
  }
}
