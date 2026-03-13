/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rpc.trpc;

import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.Reified;
import io.jooby.StatusCode;

/**
 * A specialized error handler that formats exceptions into tRPC-compliant JSON responses.
 *
 * <p>This handler strictly listens for requests where the path begins with {@code /trpc/}. When an
 * exception occurs during a tRPC procedure, this handler ensures the response matches the exact
 * JSON envelope expected by the frontend {@code @trpc/client}, preventing the client from crashing
 * due to unexpected HTML or plain-text error pages.
 *
 * <p><b>Custom Exception Mapping:</b>
 *
 * <p>By default, this handler translates standard Jooby {@link StatusCode}s into their
 * corresponding {@link TrpcErrorCode}. However, you can register a custom {@code Map<Class<?>,
 * TrpcErrorCode>} in the Jooby registry to map your own domain-specific exceptions directly to tRPC
 * codes.
 *
 * <p>If the thrown exception is already a {@link TrpcException}, it is rendered directly without
 * modification.
 */
public class TrpcErrorHandler implements ErrorHandler {

  /**
   * Applies the tRPC error formatting if the request is a tRPC call.
   *
   * @param ctx The current routing context.
   * @param cause The exception that was thrown during the request lifecycle.
   * @param code The default HTTP status code resolved by Jooby.
   */
  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    if (ctx.getRequestPath().startsWith("/trpc/")) {
      TrpcException trpcError;
      if (cause instanceof TrpcException) {
        trpcError = (TrpcException) cause;
      } else {
        // Attempt to look up any user-defined exception mappings from the registry
        Map<Class<?>, TrpcErrorCode> customMapping =
            ctx.require(Reified.map(Class.class, TrpcErrorCode.class));

        // Extract the target procedure name from the URL path
        var procedure = ctx.getRequestPath().replace("/trpc/", "");

        // Build the tRPC exception, falling back to the default HTTP status mapping if no custom
        // map matches
        trpcError =
            new TrpcException(
                procedure, errorCode(customMapping, cause).orElse(TrpcErrorCode.of(code)), cause);
      }

      // Render the response using the exact structure expected by the @trpc/client
      ctx.setResponseCode(trpcError.getStatusCode()).render(trpcError.toMap());
    }
  }

  /**
   * Evaluates the given exception against the registered custom exception mappings.
   *
   * @param mappings A map of Exception classes to specific tRPC error codes.
   * @param x The exception to evaluate.
   * @return An {@code Optional} containing the matched {@code TrpcErrorCode}, or empty if no match
   *     is found.
   */
  private Optional<TrpcErrorCode> errorCode(Map<Class<?>, TrpcErrorCode> mappings, Throwable x) {
    for (var mapping : mappings.entrySet()) {
      if (mapping.getKey().isInstance(x)) {
        return Optional.of(mapping.getValue());
      }
    }
    return Optional.empty();
  }
}
