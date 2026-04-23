/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.instrumentation;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.jsonrpc.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * OpenTelemetry tracing middleware for JSON-RPC invocations. See <a
 * href="https://opentelemetry.io/docs/specs/semconv/rpc/rpc-spans/">rpc-spans</a>.
 *
 * <p>This invoker wraps JSON-RPC requests to automatically generate OpenTelemetry spans following
 * standard RPC semantic conventions. It records the RPC system ({@code jsonrpc}), the invoked
 * method, and the request ID.
 *
 * <h3>Error Tracking</h3>
 *
 * <p>Because the Jooby JSON-RPC pipeline catches application exceptions and transforms them into
 * {@link JsonRpcResponse} objects, this tracing middleware does not rely on try-catch blocks to
 * detect business logic failures. Instead, after the action executes, it inspects the resulting
 * response for an {@link JsonRpcResponse.ErrorDetail}.
 *
 * <ul>
 *   <li>If no error is present, the span is marked with {@link StatusCode#OK}.
 *   <li>If an error is found, the span is marked with {@link StatusCode#ERROR}, the error code is
 *       recorded, and the underlying exception (if available) is attached to the span.
 * </ul>
 *
 * @author edgar
 * @since 4.5.0
 */
public class OtelJsonRcpTracing implements JsonRpcInvoker {

  private final Tracer tracer;

  private SneakyThrows.@Nullable Consumer3<Context, JsonRpcRequest, Span> onStart;

  private SneakyThrows.@Nullable Consumer3<Context, JsonRpcRequest, Span> onEnd;

  /**
   * Creates a new OpenTelemetry JSON-RPC tracing middleware.
   *
   * @param otel The OpenTelemetry instance used to obtain the tracer.
   */
  public OtelJsonRcpTracing(OpenTelemetry otel) {
    tracer = otel.getTracer("io.jooby.jsonrpc");
  }

  /**
   * Registers a custom callback to be executed immediately after the span is started, but before
   * the JSON-RPC action is invoked. This allows you to add custom attributes to the span based on
   * the HTTP context.
   *
   * @param onStart The callback accepting the HTTP Context and the active Span.
   * @return This invoker instance for chaining.
   */
  public OtelJsonRcpTracing onStart(SneakyThrows.Consumer3<Context, JsonRpcRequest, Span> onStart) {
    this.onStart = onStart;
    return this;
  }

  /**
   * Registers a custom callback to be executed immediately before the span is ended, after the
   * JSON-RPC action has completed.
   *
   * @param onEnd The callback accepting the HTTP Context and the active Span.
   * @return This invoker instance for chaining.
   */
  public OtelJsonRcpTracing onEnd(SneakyThrows.Consumer3<Context, JsonRpcRequest, Span> onEnd) {
    this.onEnd = onEnd;
    return this;
  }

  /**
   * Wraps the JSON-RPC execution in an OpenTelemetry span.
   *
   * <p>This method starts a span, executes the downstream action, and evaluates the resulting
   * {@link JsonRpcResponse}. It handles span status updates by checking {@code rsp.getError() !=
   * null}, ensuring that gracefully handled JSON-RPC errors are properly recorded as span failures.
   *
   * @param ctx The current HTTP context.
   * @param request The incoming JSON-RPC request.
   * @param chain The next step in the invocation chain.
   * @return An Optional containing the response (or empty for a Notification).
   */
  @Override
  public @NonNull Optional<JsonRpcResponse> invoke(
      @NonNull Context ctx, @NonNull JsonRpcRequest request, JsonRpcChain chain) {
    var method = Optional.ofNullable(request.getMethod()).orElse("unknown_method");
    var span =
        tracer
            .spanBuilder(method)
            .setAttribute("rpc.system", "jsonrpc")
            .setAttribute("rpc.method", method)
            .setAttribute(
                "rpc.jsonrpc.request_id",
                Optional.ofNullable(request.getId()).map(Objects::toString).orElse(null))
            .startSpan();
    try (var scope = span.makeCurrent()) {
      if (onStart != null) {
        onStart.accept(ctx, request, span);
      }
      var result = chain.proceed(ctx, request);
      if (result.isPresent()) {
        var rsp = result.get();
        // we need to check for errored response, jsonrpc pipeline won't fire exception unless they
        // are fatal where can only be propagated
        var error = rsp.getError();
        if (error == null) {
          span.setStatus(StatusCode.OK);
        } else {
          span.setStatus(StatusCode.ERROR, error.getMessage());
          span.setAttribute("rpc.response.status_code", error.getCode());
          var cause = error.exception();
          if (cause != null) {
            span.setAttribute("error.type", cause.getClass().getName());
            span.recordException(cause);
          }
        }
      }
      return result;
    } finally {
      if (onEnd != null) {
        onEnd.accept(ctx, request, span);
      }
      span.end();
    }
  }
}
