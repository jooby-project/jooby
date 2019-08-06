package io.jooby;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Handle preflight and simple CORS requests. CORS options are set via: {@link Cors}.
 *
 * @author edgar
 * @since 0.8.0
 * @see Cors
 */
public class CorsHandler implements Route.Before {

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

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Cors.class);

  private final Cors options;

  /**
   * Creates a new {@link CorsHandler}.
   *
   * @param options Cors options, or empty for using default options.
   */
  public CorsHandler(@Nonnull final Cors options) {
    this.options = options;
  }

  public CorsHandler() {
    this(new Cors());
  }

  @Override public void apply(@Nonnull Context ctx) throws Exception {
    Value origin = ctx.header("Origin");
    if (!origin.isMissing()) {
      cors(options, ctx, origin.value());
    }
  }

  private void cors(final Cors options, final Context ctx, final String origin) throws Exception {
    if (options.allowOrigin(origin)) {
      log.debug("allowed origin: {}", origin);
      if (preflight(ctx)) {
        log.debug("handling preflight for: {}", origin);
        preflight(options, ctx, origin);
      } else {
        log.debug("handling simple options for: {}", origin);
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
            ctx.setResponseHeader(AC_EXPOSE_HEADERS, options.getExposedHeaders().stream().collect(Collectors.joining()));
          }
        }
      }
    }
  }

  private boolean preflight(final Context ctx) {
    return ctx.getMethod().equalsIgnoreCase("OPTIONS") && !ctx.header(AC_REQUEST_METHOD)
        .isMissing();
  }

  private void preflight(final Cors options, final Context ctx, final String origin) {
    /**
     * Allowed method
     */
    boolean allowMethod = ctx.header(AC_REQUEST_METHOD).toOptional()
        .map(options::allowMethod)
        .orElse(false);
    if (!allowMethod) {
      return;
    }

    /**
     * Allowed headers
     */
    List<String> headers = ctx.header(AC_REQUEST_HEADERS).toOptional().map(header ->
        Arrays.asList(header.split("\\s*,\\s*"))
    ).orElse(Collections.emptyList());
    if (!options.allowHeaders(headers)) {
      return;
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
  }
}
