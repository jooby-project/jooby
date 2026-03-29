/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static io.jooby.internal.mcp.transport.TransportConstants.TEXT_EVENT_STREAM;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_REQUEST;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import reactor.core.publisher.Mono;

/**
 * Jooby-based implementation of a stateless MCP server transport. Inspired by <a
 * href="https://github.com/modelcontextprotocol/java-sdk/blob/main/mcp-spring/mcp-spring-webmvc/src/main/java/io/modelcontextprotocol/server/transport/WebMvcStatelessServerTransport.java">WebMvcStatelessServerTransport</a>.
 *
 * @author kliushnichenko
 */
@SuppressWarnings("PMD")
public class StatelessTransportProvider implements McpStatelessServerTransport {

  private static final Logger LOG = LoggerFactory.getLogger(StatelessTransportProvider.class);

  private McpStatelessServerHandler mcpHandler;
  private final McpJsonMapper mcpJsonMapper;
  private final McpTransportContextExtractor<Context> contextExtractor;
  private volatile boolean isClosing = false;

  public StatelessTransportProvider(
      Jooby app,
      McpJsonMapper jsonMapper,
      McpServerConfig serverConfig,
      McpTransportContextExtractor<Context> contextExtractor) {
    this.mcpJsonMapper = jsonMapper;
    this.contextExtractor = contextExtractor;

    var mcpEndpoint = serverConfig.getMcpEndpoint();
    app.head(mcpEndpoint, ctx -> StatusCode.OK).produces(TEXT_EVENT_STREAM);
    app.get(mcpEndpoint, this::handleGet);
    app.post(mcpEndpoint, this::handlePost);
  }

  private Object handlePost(Context ctx) {
    if (this.isClosing) {
      return SendError.serverIsShuttingDown(ctx);
    }

    if (!ctx.accept(TEXT_EVENT_STREAM) || !ctx.accept(MediaType.json)) {
      return SendError.invalidAcceptHeader(ctx, List.of(TEXT_EVENT_STREAM, MediaType.json));
    }

    McpTransportContext transportContext = this.contextExtractor.extract(ctx);
    try {
      var body = ctx.body().valueOrNull();
      if (body == null) {
        return SendError.error(
            ctx, StatusCode.BAD_REQUEST, INVALID_REQUEST, "Request body is missing");
      }
      McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, body);

      if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
        try {
          McpSchema.JSONRPCResponse jsonrpcResponse =
              this.mcpHandler
                  .handleRequest(transportContext, jsonrpcRequest)
                  .contextWrite(
                      reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                  .block();
          return jsonrpcResponse;
        } catch (Exception e) {
          LOG.error("Failed to handle request.", e);
          return SendError.internalError(ctx);
        }
      } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
        try {
          this.mcpHandler
              .handleNotification(transportContext, jsonrpcNotification)
              .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
              .block();
          return StatusCode.ACCEPTED;
        } catch (Exception e) {
          LOG.error("Failed to handle notification", e);
          return SendError.internalError(ctx);
        }
      } else {
        return SendError.badRequest(ctx, "The server accepts either requests or notifications");
      }
    } catch (IllegalArgumentException | IOException e) {
      LOG.error("Failed to deserialize message.", e);
      return SendError.badRequest(ctx, "Invalid message format");
    } catch (Exception e) {
      LOG.error("Unexpected error handling message.", e);
      return SendError.internalError(ctx);
    }
  }

  private Context handleGet(Context ctx) {
    return ctx.setResponseCode(StatusCode.METHOD_NOT_ALLOWED);
  }

  @Override
  public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
    this.mcpHandler = mcpHandler;
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Mono.fromRunnable(() -> this.isClosing = true);
  }
}
