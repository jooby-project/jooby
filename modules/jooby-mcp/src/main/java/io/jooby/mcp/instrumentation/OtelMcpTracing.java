/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.instrumentation;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jooby.Context;
import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpInvoker;
import io.jooby.mcp.McpOperation;
import io.jooby.opentelemetry.OtelContextExtractor;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class OtelMcpTracing implements McpInvoker {

  private final Tracer tracer;

  public OtelMcpTracing(OpenTelemetry openTelemetry) {
    this.tracer = openTelemetry.getTracer("io.jooby.mcp");
  }

  @Override
  public <R> R invoke(
      @Nullable McpSyncServerExchange exchange,
      @NonNull McpTransportContext transportContext,
      @NonNull McpOperation operation,
      @NonNull McpChain chain)
      throws Exception {

    // operation.getId() looks like: "tools/add_numbers" or "resources/calculator://history/{user}"
    var rawId = operation.getId();

    // Split "tools/add_numbers" into type="tools" and target="add_numbers"
    int slashIdx = rawId.indexOf('/');
    var type = slashIdx > 0 ? rawId.substring(0, slashIdx) : rawId;
    var target = slashIdx > 0 ? rawId.substring(slashIdx + 1) : null;

    // Map your prefix to the official JSON-RPC method names
    var rpcMethod =
        switch (type) {
          case "tools" -> "tools/call";
          case "prompts" -> "prompts/get";
          case "resources" -> "resources/read";
          case "completions" -> "completion/complete";
          default -> type;
        };

    // Format OTel Span Name: {mcp.method.name} {target}
    // Example: "tools/call add_numbers" or "resources/read calculator://history/{user}"
    var spanName = target != null ? rpcMethod + " " + target : rpcMethod;
    Context ctx = (Context) transportContext.get("CTX");
    var parent = ctx.require(OtelContextExtractor.class).extract(ctx);
    var builder =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.SERVER)
            .setParent(parent)
            .setAttribute("rpc.system", "mcp")
            .setAttribute("rpc.method", rpcMethod)
            .setAttribute("mcp.method.name", rpcMethod) // Fixed: mcp.method.name
            .setAttribute("rpc.service", operation.getClassName());

    if (target != null) {
      builder.setAttribute("gen_ai.operation.name", target); // Good fallback tracking
    }

    if (exchange != null && exchange.sessionId() != null) {
      builder.setAttribute("mcp.session.id", exchange.sessionId());
    }

    var request = operation.getRequest();

    // Set specific semantic attributes based on the payload
    switch (request) {
      case McpSchema.CallToolRequest callToolRequest ->
          builder.setAttribute("gen_ai.tool.name", target);
      case McpSchema.GetPromptRequest getPromptRequest ->
          builder.setAttribute("mcp.prompt.name", target);
      case McpSchema.ReadResourceRequest resourceReq ->
          builder.setAttribute("mcp.resource.uri", resourceReq.uri());
      case McpSchema.CompleteRequest completeRequest ->
          builder.setAttribute("mcp.completion.ref", target);
      default -> {}
    }

    var span = builder.startSpan();

    try (var scope = span.makeCurrent()) {
      R rsp = chain.proceed(exchange, transportContext, operation);
      if (rsp instanceof McpSchema.CallToolResult callToolResult && callToolResult.isError()) {
        traceError(operation.exception(), span);
      } else {
        span.setStatus(StatusCode.OK);
      }
      return rsp;
    } catch (Throwable cause) {
      traceError(cause, span);
      throw cause;
    } finally {
      span.end();
    }
  }

  private static void traceError(Throwable cause, Span span) {
    var message = cause != null ? cause.getMessage() : "Tool execution failed";
    span.setStatus(StatusCode.ERROR, message);
    if (cause != null) {
      span.recordException(cause);
      span.setAttribute("error.type", cause.getClass().getName());
    }
  }

  private String extractErrorMessage(List<McpSchema.Content> contentList) {
    if (contentList == null || contentList.isEmpty()) {
      return "Tool execution failed (no content provided)";
    }

    McpSchema.Content first = contentList.getFirst();

    return switch (first) {
      case McpSchema.TextContent text -> text.text();
      case McpSchema.ImageContent img -> "[Image: " + img.mimeType() + "]";
      case McpSchema.AudioContent audio -> "[Audio]";
      case McpSchema.EmbeddedResource embedded ->
          "[Embedded Resource: " + embedded.resource().uri() + "]";
      case McpSchema.ResourceLink link -> "[Resource Link: " + link.uri() + "]";
    };
  }
}
