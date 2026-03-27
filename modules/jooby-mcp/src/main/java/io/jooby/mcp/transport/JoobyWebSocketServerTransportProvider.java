/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides WebSocket transport implementation for MCP server using Jooby framework. Handles
 * bidirectional client connections, message routing, and session management.
 */
@SuppressWarnings("PMD")
public class JoobyWebSocketServerTransportProvider implements McpServerTransportProvider {

  private static final Logger LOG =
      LoggerFactory.getLogger(JoobyWebSocketServerTransportProvider.class);
  private static final String MCP_SESSION_ATTRIBUTE = "mcpSessionId";

  private final McpJsonMapper mcpJsonMapper;
  private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
  private final McpTransportContextExtractor<Context> contextExtractor;

  private McpServerSession.Factory sessionFactory;
  private final AtomicBoolean isClosing = new AtomicBoolean(false);

  /**
   * Constructs a new Jooby WebSocket transport provider instance.
   *
   * @param app The Jooby application instance to register endpoints with
   * @param serverConfig The MCP server configuration containing endpoint settings
   * @param mcpJsonMapper The MCP JSON mapper for message serialization/deserialization
   * @param contextExtractor The extractor for transport context
   */
  public JoobyWebSocketServerTransportProvider(
      Jooby app,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      McpTransportContextExtractor<Context> contextExtractor) {
    this.mcpJsonMapper = mcpJsonMapper;
    this.contextExtractor = contextExtractor;

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
  public void setSessionFactory(McpServerSession.Factory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (sessions.isEmpty()) {
      LOG.debug("No active WebSocket sessions to broadcast a message to");
      return Mono.empty();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Attempting to broadcast a message to {} active WS sessions", sessions.size());
    }

    return Flux.fromIterable(sessions.values())
        .flatMap(
            session ->
                session
                    .sendNotification(method, params)
                    .doOnError(
                        e ->
                            LOG.error(
                                "Failed to send message to WS session {}: {}",
                                session.getId(),
                                e.getMessage()))
                    .onErrorComplete())
        .then();
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Flux.fromIterable(sessions.values())
        .doFirst(
            () -> {
              isClosing.set(true);
              if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "Initiating graceful shutdown with {} active WS sessions", sessions.size());
              }
            })
        .flatMap(McpServerSession::closeGracefully)
        .doFinally(signalType -> sessions.clear())
        .then();
  }

  private void handleConnect(WebSocket ws) {
    if (isClosing.get()) {
      ws.close(WebSocketCloseStatus.SERVICE_RESTARTED);
      return;
    }

    JoobyMcpWebSocketTransport transport = new JoobyMcpWebSocketTransport(ws);
    McpServerSession session = sessionFactory.create(transport);
    String sessionId = session.getId();

    ws.attribute(MCP_SESSION_ATTRIBUTE, sessionId);
    sessions.put(sessionId, session);

    LOG.debug("New WebSocket connection established. Session ID: {}", sessionId);
  }

  private void handleMessage(WebSocket ws, WebSocketMessage msg) {
    String sessionId = ws.attribute(MCP_SESSION_ATTRIBUTE);
    if (sessionId == null) {
      LOG.warn("Received message on WebSocket without an associated MCP session");
      return;
    }

    McpServerSession session = sessions.get(sessionId);
    if (session == null) {
      LOG.warn("Received message for unknown WS session ID: {}", sessionId);
      return;
    }

    try {
      Context ctx = ws.getContext();
      McpTransportContext transportContext = this.contextExtractor.extract(ctx);
      String body = msg.value();

      McpSchema.JSONRPCMessage message =
          McpSchema.deserializeJsonRpcMessage(this.mcpJsonMapper, body);

      // Unlike HTTP POSTs, WebSockets are fully asynchronous streams, so we just subscribe
      // rather than blocking and returning an HTTP StatusCode.
      session
          .handle(message)
          .contextWrite(
              reactorCtx ->
                  reactorCtx
                      .put(io.modelcontextprotocol.common.McpTransportContext.KEY, transportContext)
                      .put("CTX", ctx))
          .subscribe(
              null,
              error ->
                  LOG.error(
                      "Error processing WS message for session {}: {}",
                      sessionId,
                      error.getMessage()));
    } catch (IOException | IllegalArgumentException e) {
      LOG.error("Failed to deserialize WS message: {}", e.getMessage());
    }
  }

  private void handleClose(WebSocket ws, WebSocketCloseStatus status) {
    String sessionId = ws.attribute(MCP_SESSION_ATTRIBUTE);
    if (sessionId != null) {
      LOG.debug(
          "WebSocket connection closed for session: {} with status: {}",
          sessionId,
          status.getCode());
      sessions.remove(sessionId);
    }
  }

  private void handleError(WebSocket ws, Throwable cause) {
    String sessionId = ws.attribute(MCP_SESSION_ATTRIBUTE);
    LOG.error("WebSocket error for session: {}", sessionId, cause);
  }

  private class JoobyMcpWebSocketTransport implements McpServerTransport {

    private final WebSocket ws;
    private volatile boolean closed = false;

    public JoobyMcpWebSocketTransport(WebSocket ws) {
      this.ws = ws;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
      return Mono.fromRunnable(
          () -> {
            try {
              if (!closed) {
                String jsonText = mcpJsonMapper.writeValueAsString(message);
                ws.send(jsonText);
              }
            } catch (Exception e) {
              LOG.error("Failed to send WebSocket message: {}", e.getMessage());
            }
          });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
      return mcpJsonMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
      return Mono.fromRunnable(this::close);
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
