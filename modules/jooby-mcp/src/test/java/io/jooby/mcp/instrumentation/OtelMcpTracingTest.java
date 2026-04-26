/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.instrumentation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpOperation;
import io.jooby.opentelemetry.OtelContextExtractor;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;

public class OtelMcpTracingTest {

  private Tracer tracer;
  private SpanBuilder spanBuilder;
  private Span span;
  private OtelMcpTracing tracing;
  private McpSyncServerExchange exchange;
  private McpTransportContext transportContext;
  private McpOperation operation;
  private McpChain chain;
  private io.jooby.Context joobyCtx;

  @BeforeEach
  void setUp() {
    OpenTelemetry otel = mock(OpenTelemetry.class);
    tracer = mock(Tracer.class);
    spanBuilder = mock(SpanBuilder.class, RETURNS_SELF);
    span = mock(Span.class);

    // Configure default stubs - important to use RETURNS_SELF or deep stubs for builders
    when(otel.getTracer("io.jooby.mcp")).thenReturn(tracer);
    when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
    when(spanBuilder.startSpan()).thenReturn(span);
    when(span.makeCurrent()).thenReturn(mock(Scope.class));

    tracing = new OtelMcpTracing(otel);

    exchange = mock(McpSyncServerExchange.class);
    transportContext = mock(McpTransportContext.class);
    operation = mock(McpOperation.class);
    chain = mock(McpChain.class);
    joobyCtx = mock(io.jooby.Context.class);

    when(transportContext.get("CTX")).thenReturn(joobyCtx);
    OtelContextExtractor extractor = mock(OtelContextExtractor.class);
    when(joobyCtx.require(OtelContextExtractor.class)).thenReturn(extractor);
    when(extractor.extract(joobyCtx)).thenReturn(io.opentelemetry.context.Context.root());

    when(operation.getClassName()).thenReturn("TestService");
  }

  @Test
  void testInvokeToolSuccess() throws Exception {
    when(operation.getId()).thenReturn("tools/add");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CallToolRequest.class));
    when(exchange.sessionId()).thenReturn("session-123");
    when(chain.proceed(any(), any(), any())).thenReturn(new Object());

    tracing.invoke(exchange, transportContext, operation, chain);

    verify(tracer).spanBuilder("tools/call add");
    verify(spanBuilder).setAttribute("mcp.session.id", "session-123");
    verify(spanBuilder).setAttribute("gen_ai.tool.name", "add");
    verify(span).setStatus(StatusCode.OK);
    verify(span).end();
  }

  @Test
  void testInvokeResourcesAndPrompts() throws Exception {
    // 1. Resources
    when(operation.getId()).thenReturn("resources/uri");
    McpSchema.ReadResourceRequest resReq = mock(McpSchema.ReadResourceRequest.class);
    when(resReq.uri()).thenReturn("mcp://test");
    when(operation.getRequest()).thenReturn(resReq);

    tracing.invoke(exchange, transportContext, operation, chain);
    verify(tracer).spanBuilder("resources/read uri");
    verify(spanBuilder).setAttribute("mcp.resource.uri", "mcp://test");

    // 2. Prompts
    when(operation.getId()).thenReturn("prompts/help");
    when(operation.getRequest()).thenReturn(mock(McpSchema.GetPromptRequest.class));

    tracing.invoke(exchange, transportContext, operation, chain);
    verify(tracer).spanBuilder("prompts/get help");
    verify(spanBuilder).setAttribute("mcp.prompt.name", "help");
  }

  @Test
  void testInvokeCompletionsAndUnknown() throws Exception {
    // 1. Completions
    when(operation.getId()).thenReturn("completions/chat");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CompleteRequest.class));

    tracing.invoke(exchange, transportContext, operation, chain);
    verify(tracer).spanBuilder("completion/complete chat");
    verify(spanBuilder).setAttribute("mcp.completion.ref", "chat");

    // 2. Unknown (no slash)
    when(operation.getId()).thenReturn("ping");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CompleteRequest.class));

    tracing.invoke(exchange, transportContext, operation, chain);
    verify(tracer).spanBuilder("ping");
  }

  @Test
  void testToolErrorResult() throws Exception {
    when(operation.getId()).thenReturn("tools/fail");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CallToolRequest.class));

    McpSchema.CallToolResult errorResult = mock(McpSchema.CallToolResult.class);
    when(errorResult.isError()).thenReturn(true);
    when(chain.proceed(any(), any(), any())).thenReturn(errorResult);

    Exception cause = new RuntimeException("tool error");
    when(operation.exception()).thenReturn(cause);

    tracing.invoke(exchange, transportContext, operation, chain);

    verify(span).setStatus(StatusCode.ERROR, "tool error");
    verify(span).recordException(cause);
  }

  @Test
  void testThrownException() throws Exception {
    when(operation.getId()).thenReturn("tools/crash");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CallToolRequest.class));

    Exception crash = new RuntimeException("crash");
    when(chain.proceed(any(), any(), any())).thenThrow(crash);

    assertThrows(
        RuntimeException.class, () -> tracing.invoke(exchange, transportContext, operation, chain));

    verify(span).setStatus(StatusCode.ERROR, "crash");
    verify(span).recordException(crash);
    verify(span).end();
  }

  @Test
  void testTraceErrorWithNullCause() throws Exception {
    when(operation.getId()).thenReturn("tools/null-error");
    when(operation.getRequest()).thenReturn(mock(McpSchema.CallToolRequest.class));
    McpSchema.CallToolResult errorResult = mock(McpSchema.CallToolResult.class);
    when(errorResult.isError()).thenReturn(true);
    when(chain.proceed(any(), any(), any())).thenReturn(errorResult);
    // Explicitly null exception
    when(operation.exception()).thenReturn(null);

    tracing.invoke(exchange, transportContext, operation, chain);

    // Checks "Tool execution failed" fallback in traceError
    verify(span).setStatus(StatusCode.ERROR, "Tool execution failed");
  }
}
