/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.exception.MissingValueException;

/**
 * Global Tier 1 Dispatcher for JSON-RPC 2.0 requests. *
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
 * install(new JsonRpcDispatcher());
 * services().put(JsonRpcService.class, new MyServiceRpc(new MyService()));
 * }</pre>
 *
 * @author Edgar Espina
 * @since 4.0.17
 */
public class JsonRpcModule implements Extension {
  private final Logger log = LoggerFactory.getLogger(JsonRpcService.class);
  private final Map<String, JsonRpcService> services = new HashMap<>();
  private final String path;

  public JsonRpcModule(String path) {
    this.path = path;
  }

  public void add(JsonRpcService service) {
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
    app.post(path, this::handle);

    // Initialize the custom exception mapping registry
    app.getServices()
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(MissingValueException.class, JsonRpcErrorCode.INVALID_PARAMS);
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
  private Object handle(Context ctx) {
    JsonRpcRequest input;
    try {
      input = ctx.body(JsonRpcRequest.class);
    } catch (Exception e) {
      // Spec: -32700 Parse error if the JSON is physically malformed.
      return JsonRpcResponse.error(null, JsonRpcErrorCode.PARSE_ERROR, e);
    }

    List<JsonRpcResponse> responses = new ArrayList<>();

    // Look up all generated *Rpc classes registered in the service registry

    for (var request : input) {
      var fullMethod = request.getMethod();

      // Spec: -32600 Invalid Request if the method member is missing or null
      if (fullMethod == null) {
        responses.add(
            JsonRpcResponse.error(request.getId(), JsonRpcErrorCode.INVALID_REQUEST, null));
        continue;
      }

      try {
        var targetService = services.get(fullMethod);
        if (targetService != null) {
          var result = targetService.execute(ctx, request);
          // Spec: If the "id" is missing, it is a notification and no response is returned.
          if (request.getId() != null) {
            responses.add(JsonRpcResponse.success(request.getId(), result));
          }
        } else {
          // Spec: -32601 Method not found
          if (request.getId() != null) {
            responses.add(
                JsonRpcResponse.error(
                    request.getId(),
                    JsonRpcErrorCode.METHOD_NOT_FOUND,
                    "Method not found: " + fullMethod));
          }
        }
      } catch (JsonRpcException cause) {
        log(ctx, request, cause);
        // Domain-specific or protocol-level exceptions (e.g., -32602 Invalid Params)
        if (request.getId() != null) {
          responses.add(JsonRpcResponse.error(request.getId(), cause.getCode(), cause.getCause()));
        }
      } catch (Exception cause) {
        log(ctx, request, cause);
        // Spec: -32603 Internal error for unhandled application exceptions
        if (request.getId() != null) {
          responses.add(
              JsonRpcResponse.error(request.getId(), computeErrorCode(ctx, cause), cause));
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

  private void log(Context ctx, JsonRpcRequest request, Throwable cause) {
    JsonRpcErrorCode code;
    boolean hasCause = true;
    if (cause instanceof JsonRpcException rpcException) {
      code = rpcException.getCode();
      hasCause = false;
    } else {
      code = computeErrorCode(ctx, cause);
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

  private JsonRpcErrorCode computeErrorCode(Context ctx, Throwable cause) {
    JsonRpcErrorCode code;
    // Attempt to look up any user-defined exception mappings from the registry
    Map<Class<?>, JsonRpcErrorCode> customMapping =
        ctx.require(Reified.map(Class.class, JsonRpcErrorCode.class));
    code =
        errorCode(customMapping, cause)
            .orElseGet(() -> JsonRpcErrorCode.of(ctx.getRouter().errorCode(cause)));
    return code;
  }

  /**
   * Evaluates the given exception against the registered custom exception mappings.
   *
   * @param mappings A map of Exception classes to specific tRPC error codes.
   * @param x The exception to evaluate.
   * @return An {@code Optional} containing the matched {@code TrpcErrorCode}, or empty if no match
   *     is found.
   */
  private Optional<JsonRpcErrorCode> errorCode(
      Map<Class<?>, JsonRpcErrorCode> mappings, Throwable x) {
    for (var mapping : mappings.entrySet()) {
      if (mapping.getKey().isInstance(x)) {
        return Optional.of(mapping.getValue());
      }
    }
    return Optional.empty();
  }
}
