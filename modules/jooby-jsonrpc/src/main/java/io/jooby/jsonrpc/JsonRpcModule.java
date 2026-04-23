/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.*;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.exception.MissingValueException;
import io.jooby.exception.TypeMismatchException;
import io.jooby.internal.jsonrpc.JsonRpcHandler;
import io.jooby.jsonrpc.instrumentation.OtelJsonRcpTracing;

/**
 * Jooby Extension module for integrating JSON-RPC 2.0 capabilities.
 *
 * <p>This module acts as the central configuration point for setting up a JSON-RPC endpoint. It
 * registers the target {@link JsonRpcService} instances, configures the route path, maps standard
 * framework exceptions to JSON-RPC error codes, and installs the underlying request handler into
 * the Jooby application.
 *
 * <h3>Middleware Pipeline (Invoker / Chain API)</h3>
 *
 * <p>This module allows you to configure a pipeline of interceptors using the {@link
 * JsonRpcInvoker} API. By adding invokers, you create a {@link JsonRpcChain} that wraps the final
 * method execution. This is the standard way to apply cross-cutting concerns to your RPC endpoints,
 * such as:
 *
 * <ul>
 *   <li>Logging request payloads and execution times.
 *   <li>Enforcing security and authorization rules.
 *   <li>Gathering metrics and OpenTelemetry tracing.
 * </ul>
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * {
 * install(new Jackson3Module());
 * install(new JsonRpcJackson3Module());
 * * install(new JsonRpcModule(new MyServiceRpc_())
 * .invoker(new MyJsonRpcMiddleware()));
 * }
 * }</pre>
 *
 * @author Edgar Espina
 * @since 4.0.17
 */
public class JsonRpcModule implements Extension {
  private final Map<String, JsonRpcService> services = new HashMap<>();
  private final String path;
  private @Nullable JsonRpcInvoker invoker;
  private @Nullable OtelJsonRcpTracing head;

  /**
   * Creates a new JSON-RPC module at a custom HTTP path.
   *
   * @param path The HTTP path where the JSON-RPC endpoint will be mounted (e.g., {@code
   *     "/api/rpc"}).
   * @param service The primary {@link JsonRpcService} containing the RPC methods to expose.
   * @param services Additional {@link JsonRpcService} instances to expose on the same endpoint.
   */
  public JsonRpcModule(String path, JsonRpcService service, JsonRpcService... services) {
    this.path = path;
    registry(service);
    Arrays.stream(services).forEach(this::registry);
  }

  /**
   * Creates a new JSON-RPC module mounted at the default {@code "/rpc"} HTTP path.
   *
   * @param service The primary {@link JsonRpcService} containing the RPC methods to expose.
   * @param services Additional {@link JsonRpcService} instances to expose on the same endpoint.
   */
  public JsonRpcModule(JsonRpcService service, JsonRpcService... services) {
    this("/rpc", service, services);
  }

  /**
   * Adds a {@link JsonRpcInvoker} middleware to the execution pipeline.
   *
   * <p>Middlewares are composed together to form a {@link JsonRpcChain}. When multiple invokers are
   * registered, they wrap around each other, meaning the first added invoker will execute first.
   *
   * <p><strong>Tracing Priority:</strong> If the provided invoker is an instance of {@link
   * OtelJsonRcpTracing}, it is automatically promoted to the absolute head of the pipeline. This
   * guarantees that OpenTelemetry spans encompass all other middlewares and the final execution.
   *
   * @param invoker The middleware interceptor to add to the pipeline.
   * @return This module instance for fluent configuration chaining.
   */
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
   * Installs the JSON-RPC handler into the Jooby application.
   *
   * <p>This method is invoked automatically by Jooby during application startup. It resolves the
   * final middleware chain, registers the HTTP POST route at the configured path, and sets up
   * default exception mappings for standard Jooby routing errors (like missing or mismatched
   * parameters).
   *
   * @param app The Jooby application instance.
   * @throws Exception If route registration or configuration fails.
   */
  @Override
  public void install(Jooby app) throws Exception {
    if (head != null) {
      invoker = invoker == null ? head : head.then(invoker);
    }
    app.post(path, new JsonRpcHandler(services, invoker));

    // Initialize the custom exception mapping registry
    app.getServices()
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(MissingValueException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(TypeMismatchException.class, JsonRpcErrorCode.INVALID_PARAMS);
  }
}
