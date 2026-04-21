/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.Objects;
import java.util.Optional;

import io.jooby.Context;
import io.jooby.SneakyThrows;

public interface JsonRpcInvoker {

  Optional<JsonRpcResponse> invoke(
      Context ctx, JsonRpcRequest request, SneakyThrows.Supplier<Optional<JsonRpcResponse>> action)
      throws Exception;

  default JsonRpcInvoker then(JsonRpcInvoker next) {
    Objects.requireNonNull(next, "next invoker is required");
    return (ctx, request, action) ->
        JsonRpcInvoker.this.invoke(ctx, request, () -> next.invoke(ctx, request, action));
  }
}
