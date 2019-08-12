/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Handle preflight and simple CORS requests. CORS options are set via: {@link Cors}.
 *
 * @author edgar
 * @since 2.0.4
 * @see Cors
 */
public class CorsHandler implements Route.Decorator {

  private static final String ORIGIN = "Origin";

  private static final String ANY_ORIGIN = "*";

  private static final String AC_REQUEST_METHOD = "Access-Control-Request-Method";

  private static final String AC_REQUEST_HEADERS = "Access-Control-Request-Headers";

  private static final String AC_MAX_AGE = "Access-Control-Max-Age";

  private static final String AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  private static final String AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  private static final String AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  private static final String AC_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  private static final String AC_ALLOW_METHODS = "Access-Control-Allow-Methods";

  private final Cors options;

  /**
   * Creates a new {@link CorsHandler}.
   *
   * @param options Cors options, or empty for using default options.
   */
  public CorsHandler(@Nonnull final Cors options) {
    this.options = options;
  }

  /**
   * Creates a new {@link CorsHandler} with default options.
   */
  public CorsHandler() {
    this(new Cors());
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      String origin = ctx.header("Origin").valueOrNull();
      if (origin != null && options.allowOrigin(origin)) {
        if (isPreflight(ctx)) {
          if (preflight(ctx, options, origin)) {
            return ctx;
          } else {
            return ctx.send(StatusCode.FORBIDDEN);
          }
        } else if (isSimple(ctx)) {
          simple(ctx, options, origin);
        } else {
          return ctx.send(StatusCode.FORBIDDEN);
        }
      }
      return next.apply(ctx);
    };
  }

  private boolean isSimple(Context ctx) {
    return ctx.getMethod().equals(Router.GET)
        || ctx.getMethod().equals(Router.POST)
        || ctx.getMethod().equals(Router.HEAD);
  }

  private void simple(final Context ctx, final Cors options, final String origin) throws Exception {
    if ("null".equals(origin)) {
      ctx.setResponseHeader(AC_ALLOW_ORIGIN, ANY_ORIGIN);
    } else {
      ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);
      if (!options.anyHeader()) {
        ctx.setResponseHeader("Vary", ORIGIN);
      }
      if (options.getUseCredentials()) {
        ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
      }
      if (!options.getExposedHeaders().isEmpty()) {
        ctx.setResponseHeader(AC_EXPOSE_HEADERS,
            options.getExposedHeaders().stream().collect(Collectors.joining()));
      }
    }
  }

  @Nonnull @Override public Route.Decorator setRoute(@Nonnull Route route) {
    route.setHttpOptions(true);
    return this;
  }

  private boolean isPreflight(final Context ctx) {
    return ctx.getMethod().equals(Router.OPTIONS) && !ctx.header(AC_REQUEST_METHOD).isMissing();
  }

  private boolean preflight(final Context ctx, final Cors options, final String origin) {
    /**
     * Allowed method
     */
    boolean allowMethod = ctx.header(AC_REQUEST_METHOD).toOptional()
        .map(options::allowMethod)
        .orElse(false);
    if (!allowMethod) {
      return false;
    }

    /**
     * Allowed headers
     */
    List<String> headers = ctx.header(AC_REQUEST_HEADERS).toOptional().map(header ->
        Arrays.asList(header.split("\\s*,\\s*"))
    ).orElse(Collections.emptyList());
    if (!options.allowHeaders(headers)) {
      return false;
    }

    /**
     * Allowed methods
     */
    ctx.setResponseHeader(AC_ALLOW_METHODS,
        options.getMethods().stream().collect(Collectors.joining(",")));

    List<String> allowedHeaders = options.anyHeader() ? headers : options.getHeaders();
    ctx.setResponseHeader(AC_ALLOW_HEADERS,
        allowedHeaders.stream().collect(Collectors.joining(",")));

    /**
     * Allow credentials
     */
    if (options.getUseCredentials()) {
      ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
    }

    long maxAge = options.getMaxAge().getSeconds();
    if (maxAge > 0) {
      ctx.setResponseHeader(AC_MAX_AGE, maxAge);
    }

    ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);

    if (!options.anyOrigin()) {
      ctx.setResponseHeader("Vary", ORIGIN);
    }

    ctx.send(StatusCode.OK);
    return true;
  }
}
