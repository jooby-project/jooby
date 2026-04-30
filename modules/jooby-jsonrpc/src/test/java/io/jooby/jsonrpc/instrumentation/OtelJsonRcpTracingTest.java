/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.instrumentation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.SneakyThrows;
import io.jooby.jsonrpc.JsonRpcChain;
import io.jooby.jsonrpc.JsonRpcRequest;
import io.jooby.jsonrpc.JsonRpcResponse;
import io.jooby.opentelemetry.OtelContextExtractor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class OtelJsonRcpTracingTest {

  @Mock private OpenTelemetry otel;
  @Mock private Tracer tracer;
  @Mock private SpanBuilder spanBuilder;
  @Mock private Span span;
  @Mock private Scope scope;

  @Mock private Context ctx;
  @Mock private JsonRpcRequest request;
  @Mock private JsonRpcChain chain;
  @Mock private OtelContextExtractor extractor;
  @Mock private io.opentelemetry.context.Context otelContext;

  private OtelJsonRcpTracing tracing;

  @BeforeEach
  void setUp() {
    when(otel.getTracer("io.jooby.jsonrpc")).thenReturn(tracer);
    tracing = new OtelJsonRcpTracing(otel);

    // Standard OpenTelemetry fluent builder stubbing
    lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);

    // FIX: Use nullable(String.class) so null request IDs don't break the fluent chain
    lenient()
        .when(spanBuilder.setAttribute(anyString(), nullable(String.class)))
        .thenReturn(spanBuilder);

    lenient().when(spanBuilder.setParent(any())).thenReturn(spanBuilder);
    lenient().when(spanBuilder.startSpan()).thenReturn(span);
    lenient().when(span.makeCurrent()).thenReturn(scope);

    // Stub Jooby Context extraction
    lenient().when(ctx.require(OtelContextExtractor.class)).thenReturn(extractor);
    lenient().when(extractor.extract(ctx)).thenReturn(otelContext);
  }

  @Test
  void testInvokeSuccess() {
    when(request.getMethod()).thenReturn("success_method");
    when(request.getId()).thenReturn(100);

    JsonRpcResponse response = mock(JsonRpcResponse.class);
    when(response.getError()).thenReturn(null);
    when(chain.proceed(ctx, request)).thenReturn(Optional.of(response));

    tracing.invoke(ctx, request, chain);

    verify(tracer).spanBuilder("success_method");
    verify(spanBuilder).setAttribute("rpc.jsonrpc.request_id", "100");
    verify(span).setStatus(StatusCode.OK);
    verify(span).end();
  }

  @Test
  void testInvokeErrorWithoutException() {
    when(request.getMethod()).thenReturn("error_method");
    when(request.getId()).thenReturn("req-abc");

    JsonRpcResponse response = mock(JsonRpcResponse.class);
    JsonRpcResponse.ErrorDetail error = mock(JsonRpcResponse.ErrorDetail.class);

    when(error.getMessage()).thenReturn("Invalid params");
    when(error.getCode()).thenReturn(-32602);
    when(error.exception()).thenReturn(null);
    when(response.getError()).thenReturn(error);

    when(chain.proceed(ctx, request)).thenReturn(Optional.of(response));

    tracing.invoke(ctx, request, chain);

    verify(span).setStatus(StatusCode.ERROR, "Invalid params");
    verify(span).setAttribute("rpc.response.status_code", (long) -32602);
    verify(span, never()).recordException(any());
    verify(span).end();
  }

  @Test
  void testInvokeErrorWithExceptionAndNullFallbacks() {
    // Branch: method == null -> fallback to "unknown_method"
    when(request.getMethod()).thenReturn(null);

    // Branch: id == null -> fallback to null attribute
    when(request.getId()).thenReturn(null);

    JsonRpcResponse response = mock(JsonRpcResponse.class);
    JsonRpcResponse.ErrorDetail error = mock(JsonRpcResponse.ErrorDetail.class);

    when(error.getMessage()).thenReturn("Internal error");
    when(error.getCode()).thenReturn(-32603);

    RuntimeException cause = new RuntimeException("Database down");
    when(error.exception()).thenReturn(cause);
    when(response.getError()).thenReturn(error);

    when(chain.proceed(ctx, request)).thenReturn(Optional.of(response));

    tracing.invoke(ctx, request, chain);

    // Verify null fallbacks
    verify(tracer).spanBuilder("unknown_method");
    verify(spanBuilder).setAttribute("rpc.jsonrpc.request_id", (String) null);

    // Verify error recording
    verify(span).setStatus(StatusCode.ERROR, "Internal error");
    verify(span).setAttribute("error.type", "java.lang.RuntimeException");
    verify(span).recordException(cause);
    verify(span).end();
  }

  @Test
  void testInvokeNotification() {
    when(request.getMethod()).thenReturn("notification_method");
    when(request.getId()).thenReturn(null);

    // Notifications return empty optionals
    when(chain.proceed(ctx, request)).thenReturn(Optional.empty());

    Optional<JsonRpcResponse> result = tracing.invoke(ctx, request, chain);

    assertTrue(result.isEmpty());

    // The span is processed, but no status is explicitly set since there's no response object
    verify(span, never()).setStatus(any(StatusCode.class));
    verify(span, never()).setStatus(any(StatusCode.class), anyString());
    verify(span).end();
  }

  @Test
  void testCallbacksAndFatalExceptionPropagation() {
    SneakyThrows.Consumer3<Context, JsonRpcRequest, Span> onStart =
        mock(SneakyThrows.Consumer3.class);
    SneakyThrows.Consumer3<Context, JsonRpcRequest, Span> onEnd =
        mock(SneakyThrows.Consumer3.class);

    tracing.onStart(onStart).onEnd(onEnd);

    when(request.getMethod()).thenReturn("fatal_method");

    // Simulate a fatal exception bypassing the standard JSON-RPC exception handler
    RuntimeException fatalException = new RuntimeException("Fatal Crash");
    when(chain.proceed(ctx, request)).thenThrow(fatalException);

    assertThrows(RuntimeException.class, () -> tracing.invoke(ctx, request, chain));

    // Verify callbacks and span closure still happen in the finally block
    verify(onStart).accept(ctx, request, span);
    verify(onEnd).accept(ctx, request, span);
    verify(scope).close();
    verify(span).end();
  }
}
