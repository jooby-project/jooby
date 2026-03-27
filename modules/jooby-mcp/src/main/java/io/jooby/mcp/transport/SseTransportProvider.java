/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import static io.jooby.mcp.transport.TransportConstants.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides SSE transport implementation for MCP server using Jooby framework. Handles client
 * connections, message routing, and session management.
 */
@SuppressWarnings("PMD")
public class SseTransportProvider implements McpServerTransportProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SseTransportProvider.class);

  private static final String ENDPOINT_EVENT_TYPE = "endpoint";
  private static final String SESSION_ID_KEY = "sessionId";

  private final String messageEndpoint;
  private final McpJsonMapper mcpJsonMapper;
  private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
  private final McpTransportContextExtractor<Context> contextExtractor;

  private McpServerSession.Factory sessionFactory;
  private final AtomicBoolean isClosing = new AtomicBoolean(false);

  /**
   * Constructs a new Jooby Reactive SSE transport provider instance.
   *
   * @param app The Jooby application instance to register endpoints with
   * @param serverConfig The MCP server configuration containing endpoint settings
   * @param mcpJsonMapper The MCP JSON mapper for message serialization/deserialization
   */
  public SseTransportProvider(
      Jooby app,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      McpTransportContextExtractor<Context> contextExtractor) {
    this.mcpJsonMapper = mcpJsonMapper;
    this.messageEndpoint = serverConfig.getMessageEndpoint();
    this.contextExtractor = contextExtractor;
    String sseEndpoint = serverConfig.getSseEndpoint();

    app.head(sseEndpoint, ctx -> StatusCode.OK).produces(TEXT_EVENT_STREAM);
    app.sse(sseEndpoint, this::handleSseConnection);
    app.post(this.messageEndpoint, this::handleMessage);
  }

  @Override
  public void setSessionFactory(McpServerSession.Factory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (sessions.isEmpty()) {
      LOG.debug("No active sessions to broadcast a message to");
      return Mono.empty();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Attempting to broadcast a message to {} active sessions", sessions.size());
    }

    return Flux.fromIterable(sessions.values())
        .flatMap(
            session ->
                session
                    .sendNotification(method, params)
                    .doOnError(
                        e ->
                            LOG.error(
                                "Failed to send message to session {}: {}",
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
                LOG.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
              }
            })
        .flatMap(McpServerSession::closeGracefully)
        .doFinally(signalType -> sessions.clear())
        .then();
  }

  private void handleSseConnection(ServerSentEmitter sse) {
    JoobyMcpSessionTransport transport = new JoobyMcpSessionTransport(sse);
    McpServerSession session = sessionFactory.create(transport);
    String sessionId = session.getId();

    LOG.debug("New SSE connection has been established. Session ID: {}", sessionId);
    sessions.put(sessionId, session);

    sse.onClose(
        () -> {
          LOG.debug("Session with ID {} has been cancelled", sessionId);
          sessions.remove(sessionId);
        });

    LOG.debug("Sending initial endpoint event to session: {}", sessionId);
    sse.send(
        new ServerSentMessage(this.messageEndpoint + "?sessionId=" + sessionId)
            .setEvent(ENDPOINT_EVENT_TYPE));
  }

  private Object handleMessage(Context ctx) {
    if (isClosing.get()) {
      ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
      return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
          .message("Server is shutting down")
          .build();
    }

    if (ctx.query(SESSION_ID_KEY).isMissing()) {
      ctx.setResponseCode(StatusCode.BAD_REQUEST);
      return McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
          .message("Session ID missing in message endpoint")
          .build();
    }

    String sessionId = ctx.query(SESSION_ID_KEY).value();
    McpServerSession session = sessions.get(sessionId);

    if (session == null) {
      ctx.setResponseCode(StatusCode.NOT_FOUND);
      return McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
          .message("Session not found: " + sessionId)
          .build();
    }

    try {
      McpTransportContext transportContext = this.contextExtractor.extract(ctx);
      var body = ctx.body().value();
      McpSchema.JSONRPCMessage message =
          McpSchema.deserializeJsonRpcMessage(this.mcpJsonMapper, body);

      return session
          .handle(message)
          .contextWrite(
              reactorCtx ->
                  reactorCtx
                      .put(io.modelcontextprotocol.common.McpTransportContext.KEY, transportContext)
                      .put("CTX", ctx))
          .then(Mono.just((Object) StatusCode.OK))
          .onErrorResume(
              error -> {
                LOG.error("Error processing  message: {}", error.getMessage());
                return Mono.just(StatusCode.OK);
              })
          .switchIfEmpty(Mono.just((Object) StatusCode.OK))
          .block();
    } catch (IOException | IllegalArgumentException e) {
      LOG.error("Failed to deserialize message: {}", e.getMessage());
      return McpError.builder(McpSchema.ErrorCodes.PARSE_ERROR)
          .message("Invalid message format")
          .build();
    }
  }

  private class JoobyMcpSessionTransport implements McpServerTransport {

    private final ServerSentEmitter sse;

    public JoobyMcpSessionTransport(ServerSentEmitter sse) {
      this.sse = sse;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
      return Mono.fromRunnable(
          () -> {
            try {
              String jsonText = mcpJsonMapper.writeValueAsString(message);
              sse.send(new ServerSentMessage(jsonText).setEvent(MESSAGE_EVENT_TYPE));
            } catch (Exception e) {
              LOG.error("Failed to send message: {}", e.getMessage());
              sse.send(SSE_ERROR_EVENT, e.getMessage());
            }
          });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
      return mcpJsonMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
      return Mono.fromRunnable(sse::close);
    }

    @Override
    public void close() {
      sse.close();
    }
  }
}
