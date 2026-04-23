/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import java.util.Map;
import java.util.Optional;

import io.jooby.Reified;
import io.jooby.Router;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcException;

public class JsonRpcExceptionTranslator {
  private final Router router;

  public JsonRpcExceptionTranslator(Router router) {
    this.router = router;
  }

  public JsonRpcErrorCode toErrorCode(Throwable cause) {
    if (cause instanceof JsonRpcException rpcException) {
      return rpcException.getCode();
    }
    // Attempt to look up any user-defined exception mappings from the registry
    Map<Class<?>, JsonRpcErrorCode> customMapping =
        router.require(Reified.map(Class.class, JsonRpcErrorCode.class));
    return errorCode(customMapping, cause)
        .orElseGet(() -> JsonRpcErrorCode.of(router.errorCode(cause)));
  }

  /**
   * Evaluates the given exception against the registered custom exception mappings.
   *
   * @param mappings A map of Exception classes to specific tRPC error codes.
   * @param cause The exception to evaluate.
   * @return An {@code Optional} containing the matched {@code TrpcErrorCode}, or empty if no match
   *     is found.
   */
  private Optional<JsonRpcErrorCode> errorCode(
      Map<Class<?>, JsonRpcErrorCode> mappings, Throwable cause) {
    for (var mapping : mappings.entrySet()) {
      if (mapping.getKey().isInstance(cause)) {
        return Optional.of(mapping.getValue());
      }
    }
    return Optional.empty();
  }
}
