/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.StatusCode;

public class TrpcErrorHandler implements ErrorHandler {
  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    if (ctx.getRequestPath().startsWith("/trpc/")) {

      var trpcCode = TrpcErrorCode.of(code);

      Map<String, Object> errorData =
          Map.of(
              "code", trpcCode.name(),
              "httpStatus", code.value(),
              "path", ctx.getRequestPath().replace("/trpc/", ""));

      var errorDetail =
          new TrpcError.ErrorDetail(cause.getMessage(), trpcCode.getRpcCode(), errorData);
      var trpcResponse = new TrpcError(errorDetail);

      ctx.setResponseCode(code).render(trpcResponse);
    }
  }
}
