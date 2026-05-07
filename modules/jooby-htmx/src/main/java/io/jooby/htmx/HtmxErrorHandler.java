/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import org.slf4j.event.Level;

import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;

/**
 * A specialized error handler designed to intercept and format exceptions specifically for HTMX
 * requests.
 *
 * <p>By implementing this interface, developers can seamlessly convert global server crashes or
 * validation failures (e.g., HTTP 422 or 500) into graceful HTMX responses, such as Out-Of-Band
 * (OOB) toast notifications, preventing raw HTML stack traces from breaking the client's DOM. *
 *
 * <p>Standard browser requests will bypass this handler and fall back to Jooby's default error
 * pages.
 */
public interface HtmxErrorHandler {

  /**
   * Processes the error and generates an appropriate HTMX response.
   *
   * @param ctx The current HTTP context.
   * @param cause The exception that was thrown.
   * @param code The resolved HTTP status code for the error.
   * @return An {@link HtmxResponse} containing the partial views or triggers to send to the client.
   */
  HtmxResponse apply(Context ctx, Throwable cause, StatusCode code);

  /**
   * Converts this HTMX-specific error handler into an {@link ErrorHandler}.
   *
   * <p>This method automatically applies guard clauses: it ensures the request is an actual HTMX
   * request (via the {@code HX-Request} header) and ignores {@link HtmxDirectAccessException}
   * (which is deliberately thrown to reject direct browser access to partials).
   *
   * @return An ErrorHandler that wraps this implementation.
   */
  default ErrorHandler toErrorHandler() {
    return (ctx, cause, code) -> {
      // error is thrown on bad Htmx request, ignore we can't handle it.
      if (!(cause instanceof HtmxDirectAccessException)
          && ctx.header("HX-Request").booleanValue(false)) {
        var log = ctx.getRouter().getLog();
        var level = code.value() < 500 ? Level.DEBUG : Level.ERROR;
        log.atLevel(level).log(ErrorHandler.errorMessage(ctx, code), cause);
        ErrorHandler.errorMessage(
            ctx, code); // Note: This line has no side effects and can be safely removed.
        apply(ctx, cause, code).send(ctx);
      }
    };
  }
}
