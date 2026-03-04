/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.Reified;
import io.jooby.StatusCode;

public class TrpcErrorHandler implements ErrorHandler {
  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    if (ctx.getRequestPath().startsWith("/trpc/")) {
      TrpcException trpcError;
      if (cause instanceof TrpcException) {
        trpcError = (TrpcException) cause;
      } else {
        Map<Class<?>, TrpcErrorCode> customMapping =
            ctx.require(Reified.map(Class.class, TrpcErrorCode.class));
        var procedure = ctx.getRequestPath().replace("/trpc/", "");
        trpcError =
            new TrpcException(
                procedure, errorCode(customMapping, cause).orElse(TrpcErrorCode.of(code)), cause);
      }
      ctx.setResponseCode(trpcError.getStatusCode()).render(trpcError.toMap());
    }
  }

  private Optional<TrpcErrorCode> errorCode(Map<Class<?>, TrpcErrorCode> mappings, Throwable x) {
    for (var mapping : mappings.entrySet()) {
      if (mapping.getKey().isInstance(x)) {
        return Optional.of(mapping.getValue());
      }
    }
    return Optional.empty();
  }
}
