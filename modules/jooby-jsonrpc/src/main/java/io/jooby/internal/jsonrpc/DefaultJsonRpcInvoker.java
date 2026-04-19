/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import org.jspecify.annotations.NonNull;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.jsonrpc.JsonRpcInvoker;
import io.jooby.jsonrpc.JsonRpcRequest;

public class DefaultJsonRpcInvoker implements JsonRpcInvoker {
  @Override
  public <R> R invoke(
      @NonNull Context ctx,
      @NonNull JsonRpcRequest request,
      SneakyThrows.@NonNull Supplier<R> action) {
    try {
      return action.get();
    } catch (Throwable cause) {
      throw SneakyThrows.propagate(cause);
    }
  }
}
