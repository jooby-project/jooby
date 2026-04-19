/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.Objects;

import io.jooby.Context;
import io.jooby.SneakyThrows;

public interface JsonRpcInvoker {

  Object invoke(Context ctx, JsonRpcRequest request, SneakyThrows.Supplier<Object> action)
      throws Exception;

  default JsonRpcInvoker then(JsonRpcInvoker next) {
    Objects.requireNonNull(next, "next invoker is required");
    return new JsonRpcInvoker() {
      @Override
      public Object invoke(
          Context ctx, JsonRpcRequest request, SneakyThrows.Supplier<Object> action)
          throws Exception {
        return JsonRpcInvoker.this.invoke(ctx, request, () -> next.invoke(ctx, request, action));
      }
    };
  }
}
