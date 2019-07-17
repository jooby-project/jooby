package io.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  private final Cors cors;

  /**
   * Creates a new {@link CorsHandler}.
   *
   * @param cors Cors options, or empty for using default options.
   */
  public CorsHandler(@Nonnull final Cors cors) {
    this.cors = cors;
  }

  @Override public void apply(@Nonnull Context ctx) throws Exception {
    Optional<String> origin = ctx.header("Origin").toOptional();
    if (cors.enabled() && origin.isPresent()) {
      cors(cors, ctx, origin.get());
    }
  }

  private void cors(final Cors cors, final Context ctx, final String origin) throws Exception {
    if (cors.allowOrigin(origin)) {
      log.debug("allowed origin: {}", origin);
      if (preflight(ctx)) {
        log.debug("handling preflight for: {}", origin);
        preflight(cors, ctx, origin);
      } else {
        log.debug("handling simple cors for: {}", origin);
        if ("null".equals(origin)) {
          ctx.setResponseHeader(AC_ALLOW_ORIGIN, ANY_ORIGIN);
        } else {
          ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);
          if (!cors.anyOrigin()) {
            ctx.setResponseHeader("Vary", ORIGIN);
          }
          if (cors.credentials()) {
            ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
          }
          if (!cors.exposedHeaders().isEmpty()) {
            ctx.setResponseHeader(AC_EXPOSE_HEADERS, cors.exposedHeaders().stream().collect(Collectors.joining()));
          }
        }
      }
    }
  }

  private boolean preflight(final Context ctx) {
    return ctx.getMethod().equalsIgnoreCase("OPTIONS") && !ctx.header(AC_REQUEST_METHOD)
        .isMissing();
  }

  private void preflight(final Cors cors, final Context ctx, final String origin) {
    /**
     * Allowed method
     */
    boolean allowMethod = ctx.header(AC_REQUEST_METHOD).toOptional()
        .map(cors::allowMethod)
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
    if (!cors.allowHeaders(headers)) {
      return;
    }

    /**
     * Allowed methods
     */
    ctx.setResponseHeader(AC_ALLOW_METHODS,
        cors.allowedMethods().stream().collect(Collectors.joining(",")));

    List<String> allowedHeaders = cors.anyHeader() ? headers : cors.allowedHeaders();
    ctx.setResponseHeader(AC_ALLOW_HEADERS,
        allowedHeaders.stream().collect(Collectors.joining(",")));

    /**
     * Allow credentials
     */
    if (cors.credentials()) {
      ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
    }

    if (cors.maxAge() > 0) {
      ctx.setResponseHeader(AC_MAX_AGE, cors.maxAge());
    }

    ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);

    if (!cors.anyOrigin()) {
      ctx.setResponseHeader("Vary", ORIGIN);
    }

    ctx.send(StatusCode.OK);
  }
}
