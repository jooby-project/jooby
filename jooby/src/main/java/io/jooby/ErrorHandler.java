/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

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
  @Nonnull void apply(@Nonnull Context ctx, @Nonnull Throwable cause,
      @Nonnull StatusCode code);

  /**
   * Chain this error handler with next and produces a new error handler.
   *
   * @param next Next error handler.
   * @return A new error handler.
   */
  @Nonnull default ErrorHandler then(@Nonnull ErrorHandler next) {
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
  static @Nonnull String errorMessage(@Nonnull Context ctx, @Nonnull StatusCode statusCode) {
    return new StringBuilder()
        .append(ctx.getMethod())
        .append(" ")
        .append(ctx.getRequestPath())
        .append(" ")
        .append(statusCode.value())
        .append(" ")
        .append(statusCode.reason())
        .toString();
  }

  /**
   * Creates a default error handler.
   * @return Default error handler.
   */
  static @Nonnull DefaultErrorHandler create() {
    return new DefaultErrorHandler();
  }
}
