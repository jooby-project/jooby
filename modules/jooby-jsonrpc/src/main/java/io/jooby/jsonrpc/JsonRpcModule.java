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
import io.jooby.internal.jsonrpc.JsonRpcHandler;
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
    app.post(path, new JsonRpcHandler(services, new JsonRpcExceptionTranslator(app), invoker));

    // Initialize the custom exception mapping registry
    app.getServices()
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(MissingValueException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(TypeMismatchException.class, JsonRpcErrorCode.INVALID_PARAMS);
  }
}
