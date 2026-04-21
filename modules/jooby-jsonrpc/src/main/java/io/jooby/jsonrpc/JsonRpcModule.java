/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.*;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.jsonrpc.JsonRpcExceptionTranslator;
import io.jooby.internal.jsonrpc.JsonRpcExecutor;
import io.jooby.jsonrpc.instrumentation.OtelJsonRcpTracing;

/**
 * Global Tier 1 Dispatcher for JSON-RPC 2.0 requests.
 *
 * <p>This dispatcher acts as the central entry point for all JSON-RPC traffic. It manages the
 * lifecycle of a request by:
 *
 * <ul>
 *   <li>Parsing the incoming body into a {@link JsonRpcRequest} (supporting both single and batch
 *       shapes).
 *   <li>Iterating through registered {@link JsonRpcService} instances to find a matching namespace.
 *   <li>Handling <strong>Notifications</strong> (requests without an {@code id}) by suppressing
 *       responses.
 *   <li>Unifying batch results into a single JSON array or a single object response as per the
 *       spec.
 * </ul>
 *
 * <p>*
 *
 * <p>Usage:
 *
 * <pre>{@code
 * install(new Jackson3Module());
 *
 * install(new JsonRpcJackson3Module());
 *
 * install(new JsonRpcModule(new MyServiceRpc_()));
 *
 * }</pre>
 *
 * @author Edgar Espina
 * @since 4.0.17
 */
public class JsonRpcModule implements Extension {
  private final Logger log = LoggerFactory.getLogger(JsonRpcService.class);
  private final Map<String, JsonRpcService> services = new HashMap<>();
  private final String path;
  private @Nullable JsonRpcInvoker invoker;
  private @Nullable OtelJsonRcpTracing head;
  private JsonRpcExceptionTranslator exceptionTranslator;

  public JsonRpcModule(String path, JsonRpcService service, JsonRpcService... services) {
    this.path = path;
    registry(service);
    Arrays.stream(services).forEach(this::registry);
  }

  public JsonRpcModule(JsonRpcService service, JsonRpcService... services) {
    this("/rpc", service, services);
  }

  public JsonRpcModule invoker(JsonRpcInvoker invoker) {
    if (invoker instanceof OtelJsonRcpTracing otel) {
      // otel goes first:
      this.head = otel;
    } else {
      if (this.invoker != null) {
        this.invoker = invoker.then(this.invoker);
      } else {
        this.invoker = invoker;
      }
    }
    return this;
  }

  private void registry(JsonRpcService service) {
    for (var method : service.getMethods()) {
      this.services.put(method, service);
    }
  }

  /**
   * Installs the JSON-RPC handler at the default {@code /rpc} endpoint.
   *
   * @param app The Jooby application instance.
   * @throws Exception If registration fails.
   */
  @Override
  public void install(Jooby app) throws Exception {
    if (head != null) {
      invoker = invoker == null ? head : head.then(invoker);
    }
    app.post(path, this::handle);

    exceptionTranslator = new JsonRpcExceptionTranslator(app);
    app.getServices().put(JsonRpcExceptionTranslator.class, exceptionTranslator);
    // Initialize the custom exception mapping registry
    app.getServices()
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(MissingValueException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(TypeMismatchException.class, JsonRpcErrorCode.INVALID_PARAMS);
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
  private Object handle(Context ctx) throws Exception {
    JsonRpcRequest input;
    try {
      input = ctx.body(JsonRpcRequest.class);
    } catch (Exception cause) {
      var badRequest = new JsonRpcRequest();
      badRequest.setMethod(JsonRpcRequest.UNKNOWN_METHOD);
      var parseError = JsonRpcResponse.error(null, JsonRpcErrorCode.PARSE_ERROR, cause);
      if (head != null) {
        // Manually handle bad request for otel
        return head.invoke(ctx, badRequest, () -> Optional.of(parseError));
      }
      log(badRequest, cause);
      return parseError;
    }

    List<JsonRpcResponse> responses = new ArrayList<>();

    // Look up all generated *Rpc classes registered in the service registry
    for (var request : input) {
      try {
        var target = new JsonRpcExecutor(services, ctx, request);
        var response = invoker == null ? target.get() : invoker.invoke(ctx, request, target);
        response.ifPresent(responses::add);
      } catch (JsonRpcException cause) {
        log(request, cause);
        // Domain-specific or protocol-level exceptions (e.g., -32602 Invalid Params)
        if (request.getId() != null) {
          responses.add(JsonRpcResponse.error(request.getId(), cause.getCode(), cause.getCause()));
        }
      } catch (Exception cause) {
        log(request, cause);
        // Spec: -32603 Internal error for unhandled application exceptions
        if (request.getId() != null) {
          responses.add(
              JsonRpcResponse.error(
                  request.getId(), exceptionTranslator.toErrorCode(cause), cause));
        }
      }
    }

    // Handle the case where all requests in a batch were notifications
    if (responses.isEmpty()) {
      ctx.setResponseCode(StatusCode.NO_CONTENT);
      return "";
    }

    // Spec: Return an array only if the original request was a batch
    return input.isBatch() ? responses : responses.getFirst();
  }

  private void log(JsonRpcRequest request, Throwable cause) {
    JsonRpcErrorCode code;
    boolean hasCause = true;
    if (cause instanceof JsonRpcException rpcException) {
      code = rpcException.getCode();
      hasCause = false;
    } else {
      code = exceptionTranslator.toErrorCode(cause);
    }
    var type = code == JsonRpcErrorCode.INTERNAL_ERROR ? "server" : "client";
    var message = "JSON-RPC {} error [{} {}] on method '{}' (id: {})";
    switch (code) {
      case INTERNAL_ERROR ->
          log.error(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
      case UNAUTHORIZED, FORBIDDEN, NOT_FOUND_ERROR ->
          log.debug(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
      default -> {
        if (hasCause) {
          log.warn(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId(),
              cause);
        } else {
          log.debug(
              message,
              type,
              code.getCode(),
              code.getMessage(),
              request.getMethod(),
              request.getId());
        }
      }
    }
  }
}
