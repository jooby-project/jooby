/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Handler for static resources represented by the {@link Asset} contract.
 *
 * It has built-in support for static-static as well as SPAs (single page applications).
 *
 * @author edgar
 * @since 2.0.0
 */
public class AssetHandler implements Route.Handler {
  private static final int ONE_SEC = 1000;

  private final AssetSource[] sources;

  private final CacheControl defaults = CacheControl.defaults();

  private String filekey;

  private String fallback;

  private Function<String, CacheControl> cacheControl = path -> defaults;

  /**
   * Creates a new asset handler that fallback to the given fallback asset when the asset
   * is not found. Instead of produces a <code>404</code> its fallback to the given asset.
   *
   * <pre>{@code
   * {
   *    assets("/?*", new AssetHandler("index.html", AssetSource.create(Paths.get("...")));
   * }
   * }</pre>
   *
   * The fallback option makes the asset handler to work like a SPA (Single-Application-Page).
   *
   * @param fallback Fallback asset.
   * @param sources Asset sources.
   */
  public AssetHandler(@Nonnull String fallback, AssetSource... sources) {
    this.fallback = fallback;
    this.sources = sources;
  }

  /**
   * Creates a new asset handler.
   *
   * @param sources Asset sources.
   */
  public AssetHandler(AssetSource... sources) {
    this.sources = sources;
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) throws Exception {
    final String resolvedPath;
    String filepath = ctx.pathMap().getOrDefault(filekey, "index.html");
    Asset asset = resolve(filepath);
    if (asset == null) {
      if (fallback != null) {
        asset = resolve(fallback);
      }
      // Still null?
      if (asset == null) {
        ctx.send(StatusCode.NOT_FOUND);
        return ctx;
      } else {
        resolvedPath = fallback;
      }
    } else {
      resolvedPath = filepath;
    }

    CacheControl cacheParams = cacheControl.apply(resolvedPath);

    // handle If-None-Match
    if (cacheParams.isEtag()) {
      String ifnm = ctx.header("If-None-Match").value((String) null);
      if (ifnm != null && ifnm.equals(asset.getEtag())) {
        ctx.send(StatusCode.NOT_MODIFIED);
        asset.close();
        return ctx;
      } else {
        ctx.setResponseHeader("ETag", asset.getEtag());
      }
    }

    // Handle If-Modified-Since
    if (cacheParams.isLastModified()) {
      long lastModified = asset.getLastModified();
      if (lastModified > 0) {
        long ifms = ctx.header("If-Modified-Since").longValue(-1);
        if (lastModified / ONE_SEC <= ifms / ONE_SEC) {
          ctx.send(StatusCode.NOT_MODIFIED);
          asset.close();
          return ctx;
        }
        ctx.setResponseHeader("Last-Modified", Instant.ofEpochMilli(lastModified));
      }
    }

    // cache control
    if (cacheParams.getMaxAge() >= 0) {
      ctx.setResponseHeader("Cache-Control", "max-age=" + cacheParams.getMaxAge());
    } else if (cacheParams.getMaxAge() == CacheControl.NO_CACHE) {
      ctx.setResponseHeader("Cache-Control", "no-store, must-revalidate");
    }

    long length = asset.getSize();
    if (length != -1) {
      ctx.setResponseLength(length);
    }
    ctx.setResponseType(asset.getContentType());
    return ctx.send(asset.stream());
  }

  /**
   * Turn on/off e-tag support.
   *
   * @param etag True for turning on.
   * @return This handler.
   */
  public AssetHandler setETag(boolean etag) {
    defaults.setETag(etag);
    return this;
  }

  /**
   * Turn on/off handling of {@code If-Modified-Since} header.
   *
   * @param lastModified True for turning on. Default is: true.
   * @return This handler.
   */
  public AssetHandler setLastModified(boolean lastModified) {
    defaults.setLastModified(lastModified);
    return this;
  }

  /**
   * Set cache-control header with the given max-age value. If max-age is greater than 0.
   *
   * @param maxAge Max-age value in seconds.
   * @return This handler.
   */
  public AssetHandler setMaxAge(long maxAge) {
    defaults.setMaxAge(maxAge);
    return this;
  }

  /**
   * Set cache-control header with the given max-age value. If max-age is greater than 0.
   *
   * @param maxAge Max-age value in seconds.
   * @return This handler.
   */
  public AssetHandler setMaxAge(Duration maxAge) {
    defaults.setMaxAge(maxAge);
    return this;
  }

  /**
   * Set cache-control header to {@code no-store, must-revalidate}, disables e-tag
   * and {@code If-Modified-Since} header support.
   *
   * @return This handler.
   */
  public AssetHandler setNoCache() {
    defaults.setNoCache();
    return this;
  }

  /**
   * Sets a custom function that provides caching configuration for each individual
   * asset response overriding the defaults set in {@link AssetHandler}.
   *
   * @param cacheControl a cache configuration provider function.
   * @return this instance.
   * @see CacheControl
   */
  public AssetHandler cacheControl(@Nonnull Function<String, CacheControl> cacheControl) {
    this.cacheControl = requireNonNull(cacheControl);
    return this;
  }

  private Asset resolve(String filepath) {
    for (AssetSource source : sources) {
      Asset asset = source.resolve(filepath);
      if (asset != null) {
        return asset;
      }
    }
    return null;
  }

  @Override public void setRoute(Route route) {
    List<String> keys = route.getPathKeys();
    this.filekey = keys.size() == 0 ? route.getPattern().substring(1) : keys.get(0);
    // NOTE: It send an inputstream we don't need a renderer
    route.setReturnType(Context.class);
  }
}
