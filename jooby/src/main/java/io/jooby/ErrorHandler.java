/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Catch and encode application errors.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface ErrorHandler {

  /**
   * Produces an error response using the given exception and status code.
   *
   * @param ctx Web context.
   * @param cause Application error.
   * @param code Status code.
   */
  void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code);

  /**
   * Chain this error handler with next and produces a new error handler.
   *
   * @param next Next error handler.
   * @return A new error handler.
   */
  @NonNull default ErrorHandler then(@NonNull ErrorHandler next) {
    return (ctx, cause, statusCode) -> {
      apply(ctx, cause, statusCode);
      if (!ctx.isResponseStarted()) {
        next.apply(ctx, cause, statusCode);
      }
    };
  }

  /**
   * Build a line error message that describe the current web context and the status code.
   *
   * <pre>GET /path Status-Code Status-Reason</pre>
   *
   * @param ctx Web context.
   * @param statusCode Status code.
   * @return Single line message.
   */
  static @NonNull String errorMessage(@NonNull Context ctx, @NonNull StatusCode statusCode) {
    return ctx.getMethod()
        + " "
        + ctx.getRequestPath()
        + " "
        + statusCode.value()
        + " "
        + statusCode.reason();
  }

  /**
   * Creates a default error handler.
   *
   * @return Default error handler.
   */
  static @NonNull DefaultErrorHandler create() {
    return new DefaultErrorHandler();
  }
}
