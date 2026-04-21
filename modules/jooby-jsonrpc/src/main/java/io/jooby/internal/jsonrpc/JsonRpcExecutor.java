/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import java.util.Map;
import java.util.Optional;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;
import io.jooby.jsonrpc.JsonRpcService;

public class JsonRpcExecutor implements SneakyThrows.Supplier<Optional<JsonRpcResponse>> {
  private final Map<String, JsonRpcService> services;
  private final Context ctx;
  private final JsonRpcRequest request;

  public JsonRpcExecutor(
      Map<String, JsonRpcService> services, Context ctx, JsonRpcRequest request) {
    this.services = services;
    this.ctx = ctx;
    this.request = request;
  }

  @Override
  public Optional<JsonRpcResponse> tryGet() throws Exception {
    var fullMethod = request.getMethod();
    if (fullMethod == null) {
      return Optional.of(
          JsonRpcResponse.error(request.getId(), JsonRpcErrorCode.INVALID_REQUEST, null));
    }
    var targetService = services.get(fullMethod);
    if (targetService != null) {
      var result = targetService.execute(ctx, request);
      return request.getId() != null
          ? Optional.of(JsonRpcResponse.success(request.getId(), result))
          : Optional.empty();
    }
    return Optional.of(
        JsonRpcResponse.error(
            request.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found: " + fullMethod));
  }
}
