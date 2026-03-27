/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import java.io.IOException;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import reactor.core.publisher.Mono;

@SuppressWarnings("PMD")
public class WebSocketTransportProvider extends AbstractMcpTransportProvider {

  private static final String MCP_SESSION_ATTRIBUTE = "mcpSessionId";

  public WebSocketTransportProvider(
      Jooby app,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      McpTransportContextExtractor<Context> contextExtractor) {
    super(mcpJsonMapper, contextExtractor);
    String wsEndpoint = serverConfig.getMcpEndpoint();

    app.ws(
        wsEndpoint,
        (ctx, ws) -> {
          ws.onConnect(this::handleConnect);
          ws.onMessage(this::handleMessage);
          ws.onClose(this::handleClose);
          ws.onError(this::handleError);
        });
  }

  @Override
  protected String transportName() {
    return "WebSocket";
  }

  private void handleConnect(WebSocket ws) {
    if (isClosing.get()) {
      ws.close(WebSocketCloseStatus.SERVICE_RESTARTED);
      return;
    }

    JoobyMcpWebSocketTransport transport = new JoobyMcpWebSocketTransport(mcpJsonMapper, ws);
    McpServerSession session = sessionFactory.create(transport);
    String sessionId = session.getId();

    ws.attribute(MCP_SESSION_ATTRIBUTE, sessionId);
    sessions.put(sessionId, session);
    log.debug("New WebSocket connection established. Session ID: {}", sessionId);
  }

  private void handleMessage(WebSocket ws, WebSocketMessage msg) {
    String sessionId = ws.attribute(MCP_SESSION_ATTRIBUTE);
    if (sessionId == null || !sessions.containsKey(sessionId)) {
      log.warn("Received message on unknown or orphaned WS session ID: {}", sessionId);
      return;
    }

    try {
      Context ctx = ws.getContext();
      McpTransportContext transportContext = this.contextExtractor.extract(ctx);
      McpSchema.JSONRPCMessage message =
          McpSchema.deserializeJsonRpcMessage(this.mcpJsonMapper, msg.value());

      sessions
          .get(sessionId)
          .handle(message)
          .contextWrite(
              reactorCtx ->
                  reactorCtx.put(McpTransportContext.KEY, transportContext).put("CTX", ctx))
          .subscribe(
              null,
              error ->
                  log.error(
                      "Error processing WS message for {}: {}", sessionId, error.getMessage()));
    } catch (IOException | IllegalArgumentException e) {
      log.error("Failed to deserialize WS message: {}", e.getMessage());
    }
  }

  private void handleClose(WebSocket ws, WebSocketCloseStatus status) {
    String sessionId = ws.attribute(MCP_SESSION_ATTRIBUTE);
    if (sessionId != null) {
      log.debug(
          "WebSocket connection closed for session: {} with status: {}",
          sessionId,
          status.getCode());
      sessions.remove(sessionId);
    }
  }

  private void handleError(WebSocket ws, Throwable cause) {
    log.error("WebSocket error for session: {}", ws.attribute(MCP_SESSION_ATTRIBUTE), cause);
  }

  private static class JoobyMcpWebSocketTransport extends AbstractMcpTransport {
    private final WebSocket ws;
    private volatile boolean closed = false;

    public JoobyMcpWebSocketTransport(McpJsonMapper mcpJsonMapper, WebSocket ws) {
      super(mcpJsonMapper);
      this.ws = ws;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
      return Mono.fromRunnable(
          () -> {
            try {
              if (!closed) ws.send(mcpJsonMapper.writeValueAsString(message));
            } catch (Exception e) {
              log.error("Failed to send WebSocket message: {}", e.getMessage());
            }
          });
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        ws.close(WebSocketCloseStatus.NORMAL);
      }
    }
  }
}
