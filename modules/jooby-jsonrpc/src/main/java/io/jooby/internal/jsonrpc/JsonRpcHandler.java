/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jsonrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.annotation.Generated;
import io.jooby.jsonrpc.JsonRpcInvoker;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;
import io.jooby.jsonrpc.JsonRpcService;

public class JsonRpcHandler implements Route.Handler {
  private final Map<String, JsonRpcService> services;
  private final JsonRpcExceptionTranslator exceptionTranslator;
  private final HashMap<Class<?>, Logger> loggers;
  private final JsonRpcInvoker invoker;

  public JsonRpcHandler(
      Map<String, JsonRpcService> services,
      JsonRpcExceptionTranslator exceptionTranslator,
      JsonRpcInvoker invoker) {
    this.services = services;
    this.exceptionTranslator = exceptionTranslator;
    this.invoker = invoker;
    this.loggers = new HashMap<>();
    loggers.put(JsonRpcService.class, LoggerFactory.getLogger(JsonRpcService.class));
    services
        .values()
        .forEach(
            service -> {
              var generated = service.getClass().getAnnotation(Generated.class);
              loggers.put(service.getClass(), LoggerFactory.getLogger(generated.value()));
            });
  }

  /**
   * Main handler for the JSON-RPC protocol. *
   *
   * <p>This method implements the flattened iteration logic. Because {@link JsonRpcRequest}
   * implements {@code Iterable}, this handler treats single requests and batch requests identically
   * during processing.
   *
   * @param ctx The current Jooby context.
   * @return A single {@link JsonRpcResponse}, a {@code List} of responses for batches, or an empty
   *     string for notifications.
   */
  @Override
  public @NonNull Object apply(@NonNull Context ctx) throws Exception {
    JsonRpcRequest input;
    Exception parseError = null;
    try {
      input = ctx.body(JsonRpcRequest.class);
    } catch (Exception cause) {
      // still execute the handler/pipeline so we can log the error properly
      input = JsonRpcRequest.BAD_REQUEST;
      parseError = cause;
    }

    var responses = new ArrayList<JsonRpcResponse>();
    var executor = new JsonRpcExecutor(loggers, services, exceptionTranslator, parseError);

    // Look up all generated *Rpc classes registered in the service registry
    for (var request : input) {
      var response =
          invoker == null ? executor.proceed(ctx, request) : invoker.invoke(ctx, request, executor);
      response.ifPresent(responses::add);
    }

    // Handle the case where all requests in a batch were notifications
    if (responses.isEmpty()) {
      return ctx.send(StatusCode.NO_CONTENT);
    }

    // Spec: Return an array only if the original request was a batch
    return input.isBatch() ? responses : responses.getFirst();
  }

  @Override
  public void setRoute(Route route) {
    route.setAttribute("jsonrpc", true);
  }
}
