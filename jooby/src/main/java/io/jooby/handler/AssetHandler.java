/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.exception.TypeMismatchException;

/**
 * Handler for static resources represented by the {@link Asset} contract.
 *
 * <p>It has built-in support for static-static as well as SPAs (single page applications).
 *
 * @author edgar
 * @since 2.0.0
 */
public class AssetHandler implements Route.Handler {
  private static final SneakyThrows.Consumer<Context> NOT_FOUND =
      ctx -> ctx.send(StatusCode.NOT_FOUND);
  private static final int ONE_SEC = 1000;

  private final AssetSource[] sources;

  /** Cache control defaults value. */
  private final CacheControl defaults = CacheControl.defaults();

  private String filekey;

  /** File to fallback in case of not found matching path. */
  private String fallback;

  /** Cache control function. */
  private Function<String, CacheControl> cacheControl = path -> defaults;

  private Function<Asset, MediaType> mediaTypeResolver = Asset::getContentType;
  private SneakyThrows.Consumer<Context> notFound = NOT_FOUND;

  /**
   * Creates a new asset handler that fallback to the given fallback asset when the asset is not
   * found. Instead of produces a <code>404</code> its fallback to the given asset.
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
   * @param sources Asset sources. At least one source is required.
   */
  public AssetHandler(String fallback, AssetSource... sources) {
    this.fallback = fallback;
    this.sources = checkSource(sources);
  }

  /**
   * Creates a new asset handler.
   *
   * @param sources Asset sources. At least one source is required.
   */
  public AssetHandler(AssetSource... sources) {
    this.sources = checkSource(sources);
  }

  @Override
  public Object apply(Context ctx) throws Exception {
    final String resolvedPath;
    String filepath = ctx.path(filekey).value("index.html");
    Asset asset = resolve(filepath);
    if (asset == null) {
      if (fallback != null) {
        asset = resolve(fallback);
      }
      // Still null?
      if (asset == null) {
        notFound.accept(ctx);
        return ctx;
      } else {
        resolvedPath = fallback;
      }
    } else {
      resolvedPath = filepath;
    }

    CacheControl cacheParams = cacheControl.apply(resolvedPath);

    // handle If-None-Match
    String ifnm = ctx.header("If-None-Match").valueOrNull();
    if (cacheParams.isEtag()) {
      if (ifnm != null && etagMatches(ifnm, asset.getEtag())) {
        ctx.send(StatusCode.NOT_MODIFIED);
        asset.close();
        return ctx;
      } else {
        ctx.setResponseHeader("ETag", asset.getEtag());
      }
    }

    // Handle If-Modified-Since, which is ignored when If-None-Match is present (RFC 9110, 13.2.2)
    if (cacheParams.isLastModified()) {
      long lastModified = asset.getLastModified();
      if (lastModified > 0) {
        long ifms = ifnm == null ? ifModifiedSince(ctx) : -1;
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
    ctx.setResponseType(mediaTypeResolver.apply(asset));
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
   * Allow to customize the default media type and/or the charset of it.
   *
   * <pre>{@code
   * // GBK
   * var gbk = MediaType.valueOf("text/html;charset=GBK");
   *
   * Function<Asset, MediaType> overrideCharset = asset -> {
   *     var defaultType = asset.getContentType();
   *     // Choose what is best for you
   *     if (defaultType.matches(gbk)) {
   *         return gbk;
   *     }
   *     return defaultType;
   * };
   * app.assets("/3267/gbk/?*", new AssetHandler(source).setMediaTypeResolver(overrideCharset));
   *
   * }</pre>
   *
   * @param mediaTypeResolver Type resolver.
   * @return This handler.
   */
  public AssetHandler setMediaTypeResolver(Function<Asset, MediaType> mediaTypeResolver) {
    this.mediaTypeResolver =
        Objects.requireNonNull(mediaTypeResolver, "Media type resolver is required.");
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
   * Set cache-control header to {@code no-store, must-revalidate}, disables e-tag and {@code
   * If-Modified-Since} header support.
   *
   * @return This handler.
   */
  public AssetHandler setNoCache() {
    defaults.setNoCache();
    return this;
  }

  /**
   * Sets a custom function that provides caching configuration for each individual asset response
   * overriding the defaults set in {@link AssetHandler}.
   *
   * @param cacheControl a cache configuration provider function.
   * @return this instance.
   * @see CacheControl
   */
  public AssetHandler cacheControl(Function<String, CacheControl> cacheControl) {
    this.cacheControl = requireNonNull(cacheControl);
    return this;
  }

  /**
   * Sets a custom handler for <code>404</code> asset/resource. By default, generates a <code>404
   * </code> status code response.
   *
   * @param handler Handler.
   * @return This handler.
   */
  public AssetHandler notFound(SneakyThrows.Consumer<Context> handler) {
    this.notFound = handler;
    return this;
  }

  /**
   * If-None-Match is "*" or a list of entity tags, compared with the weak comparison function (RFC
   * 9110, section 13.1.2).
   */
  private static boolean etagMatches(String ifNoneMatch, String etag) {
    if (ifNoneMatch.equals(etag)) {
      return true;
    }
    String opaqueTag = opaqueTag(etag);
    for (String tag : ifNoneMatch.split(",")) {
      tag = tag.trim();
      if (tag.equals("*") || opaqueTag(tag).equals(opaqueTag)) {
        return true;
      }
    }
    return false;
  }

  private static String opaqueTag(String etag) {
    return etag.startsWith("W/") ? etag.substring(2) : etag;
  }

  /** An If-Modified-Since that is not a valid date is ignored (RFC 9110, section 13.1.3). */
  private static long ifModifiedSince(Context ctx) {
    try {
      return ctx.header("If-Modified-Since").longValue(-1);
    } catch (TypeMismatchException x) {
      return -1;
    }
  }

  private @Nullable Asset resolve(String filepath) {
    for (AssetSource source : sources) {
      Asset asset = source.resolve(filepath);
      if (asset != null) {
        return asset;
      }
    }
    return null;
  }

  @Override
  public void setRoute(Route route) {
    List<String> keys = route.getPathKeys();
    this.filekey = keys.isEmpty() ? route.getPattern().substring(1) : keys.get(0);
    //    // NOTE: It send an inputstream we don't need a renderer
    //    route.setReturnType(Context.class);
  }

  private static AssetSource[] checkSource(AssetSource[] sources) {
    if (sources.length == 0) {
      throw new IllegalArgumentException("At least one source is required.");
    }
    return sources;
  }
}
