/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import java.util.Objects;
import java.util.Optional;

import io.jooby.Context;
import io.jooby.SneakyThrows;

/**
 * Interceptor or middleware for processing JSON-RPC requests.
 *
 * <p>This interface allows you to wrap the execution of a JSON-RPC method call to apply
 * cross-cutting concerns such as logging, security, validation, or metrics.
 *
 * <h3>Exception Handling</h3>
 *
 * Implementations of this chain <strong>must never throw exceptions</strong>. According to the
 * JSON-RPC 2.0 specification, application and transport errors should be handled gracefully and
 * returned to the client as part of the response object within the {@code error} attribute.
 *
 * <p>Within the execution pipeline, the <strong>final invoker</strong> in the chain (the core
 * framework executor) automatically handles this for you. It serves as the safety net, catching
 * unhandled exceptions from the target method call, as well as protocol-level failures (such as
 * Parse Error or Invalid Request), and safely transforms them into an {@link Optional} containing a
 * {@link JsonRpcResponse} populated with the appropriate error details.
 *
 * <h3>Response Type (Optional)</h3>
 *
 * The execution returns an {@code Optional<JsonRpcResponse>} because the JSON-RPC 2.0 protocol
 * defines two distinct types of client messages:
 *
 * <ul>
 *   <li><strong>Method Calls:</strong> Requests that include an {@code id} member. The server MUST
 *       reply to these with a present {@link JsonRpcResponse} (containing either a {@code result}
 *       or an {@code error}).
 *   <li><strong>Notifications:</strong> Requests that intentionally omit the {@code id} member. The
 *       client is not expecting and cannot map a response. The server MUST NOT reply to a
 *       notification, even if an error occurs. For these requests, the chain must return {@link
 *       Optional#empty()}.
 * </ul>
 */
public interface JsonRpcInvoker {

  /**
   * Invokes the JSON-RPC request, passing control to the next invoker in the chain or to the final
   * target method.
   *
   * <p>Because the final invoker automatically catches exceptions and converts them into error
   * responses, you do not need to wrap the {@code action} in a try-catch block. Instead, if your
   * middleware needs to react to a failure (e.g., to record an error metric), you can execute the
   * action and check for an error by evaluating {@code response.get().getError() != null}.
   *
   * @param ctx The current HTTP context.
   * @param request The incoming JSON-RPC request.
   * @param action The next step in the invocation chain (or the final method execution).
   * @return An {@link Optional} containing the response for a standard method call, or {@link
   *     Optional#empty()} if the incoming request was a notification.
   */
  Optional<JsonRpcResponse> invoke(
      Context ctx, JsonRpcRequest request, SneakyThrows.Supplier<Optional<JsonRpcResponse>> action);

  /**
   * Chains this invoker with another one to form a middleware pipeline.
   *
   * @param next The next invoker to execute in the chain.
   * @return A composed {@link JsonRpcInvoker} that first executes this invoker, and delegates to
   *     the next.
   * @throws NullPointerException if the next invoker is null.
   */
  default JsonRpcInvoker then(JsonRpcInvoker next) {
    Objects.requireNonNull(next, "next invoker is required");
    return (ctx, request, action) ->
        JsonRpcInvoker.this.invoke(ctx, request, () -> next.invoke(ctx, request, action));
  }
}
