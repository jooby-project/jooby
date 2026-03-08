/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jooby.*;

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
 * *
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
public class JsonRpcDispatcher implements Extension {

  private final Map<String, JsonRpcService> services = new HashMap<>();

  public JsonRpcDispatcher(JsonRpcService... services) {
    for (JsonRpcService service : services) {
      for (String method : service.getMethods()) {
        this.services.put(method, service);
      }
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
    app.post("/rpc", this::handle);
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
  public Object handle(Context ctx) {
    JsonRpcRequest input;
    try {
      input = ctx.body(JsonRpcRequest.class);
    } catch (Exception e) {
      // Spec: -32700 Parse error if the JSON is physically malformed.
      return JsonRpcResponse.error(null, -32700, "Parse error");
    }

    List<JsonRpcResponse> responses = new ArrayList<>();

    // Look up all generated *Rpc classes registered in the service registry

    for (var request : input) {
      String fullMethod = request.getMethod();

      // Spec: -32600 Invalid Request if the method member is missing or null
      if (fullMethod == null) {
        if (request.getId() != null) {
          responses.add(JsonRpcResponse.error(request.getId(), -32600, "Invalid Request"));
        }
        continue;
      }

      try {
        JsonRpcService targetService = services.get(fullMethod);
        if (targetService != null) {
          Object result = targetService.execute(ctx, request);
          // Spec: If the "id" is missing, it is a notification and no response is returned.
          if (request.getId() != null) {
            responses.add(JsonRpcResponse.success(request.getId(), result));
          }
        } else {
          // Spec: -32601 Method not found
          if (request.getId() != null) {
            responses.add(
                JsonRpcResponse.error(request.getId(), -32601, "Method not found: " + fullMethod));
          }
        }
      } catch (JsonRpcException e) {
        // Domain-specific or protocol-level exceptions (e.g., -32602 Invalid Params)
        if (request.getId() != null) {
          responses.add(JsonRpcResponse.error(request.getId(), e.getCode(), e.getMessage()));
        }
      } catch (Exception e) {
        // Spec: -32603 Internal error for unhandled application exceptions
        if (request.getId() != null) {
          responses.add(
              JsonRpcResponse.error(request.getId(), -32603, "Internal error: " + e.getMessage()));
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
}
