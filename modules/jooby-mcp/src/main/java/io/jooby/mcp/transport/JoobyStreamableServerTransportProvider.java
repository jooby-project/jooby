/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import static io.jooby.mcp.transport.TransportConstants.*;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_REQUEST;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Jooby implementation of Streamable HTTP transport. Inspired by <a
 * href="https://github.com/modelcontextprotocol/java-sdk/blob/main/mcp-spring/mcp-spring-webmvc/src/main/java/io/modelcontextprotocol/server/transport/WebMvcStreamableServerTransportProvider.java">WebMvcStreamableServerTransportProvider</a>
 *
 * @author kliushnichenko
 */
@SuppressWarnings("PMD")
public class JoobyStreamableServerTransportProvider
    implements McpStreamableServerTransportProvider {

  private static final Logger LOG =
      LoggerFactory.getLogger(JoobyStreamableServerTransportProvider.class);

  private final boolean disallowDelete;
  private final McpJsonMapper mcpJsonMapper;
  private final ConcurrentHashMap<String, McpStreamableServerSession> sessions =
      new ConcurrentHashMap<>();
  private final McpTransportContextExtractor<Context> contextExtractor;
  private volatile boolean isClosing = false;
  private McpStreamableServerSession.Factory sessionFactory;
  private KeepAliveScheduler keepAliveScheduler;

  public JoobyStreamableServerTransportProvider(
      Jooby app,
      McpJsonMapper jsonMapper,
      McpServerConfig serverConfig,
      McpTransportContextExtractor<Context> contextExtractor) {
    Objects.requireNonNull(contextExtractor, "McpTransportContextExtractor must not be null");

    this.mcpJsonMapper = jsonMapper;
    this.disallowDelete = serverConfig.isDisallowDelete();
    this.contextExtractor = contextExtractor;

    var mcpEndpoint = serverConfig.getMcpEndpoint();

    app.head(mcpEndpoint, ctx -> StatusCode.OK).produces(TEXT_EVENT_STREAM);
    app.get(mcpEndpoint, this::handleGet);
    app.post(mcpEndpoint, this::handlePost);
    app.delete(mcpEndpoint, this::handleDelete);

    if (serverConfig.getKeepAliveInterval() != null) {
      var keepAliveInterval = Duration.ofSeconds(serverConfig.getKeepAliveInterval());
      this.keepAliveScheduler =
          KeepAliveScheduler.builder(
                  () -> isClosing ? Flux.empty() : Flux.fromIterable(this.sessions.values()))
              .initialDelay(keepAliveInterval)
              .interval(keepAliveInterval)
              .build();

      this.keepAliveScheduler.start();
    }
  }

  /**
   * Setups the listening SSE connections and message replay.
   *
   * @param ctx The Jooby context for the incoming request
   */
  private Context handleGet(Context ctx) {
    if (this.isClosing) {
      return SendError.serverIsShuttingDown(ctx);
    }

    if (!ctx.accept(TEXT_EVENT_STREAM)) {
      return SendError.invalidAcceptHeader(ctx, List.of(TEXT_EVENT_STREAM));
    }

    var transportContext = this.contextExtractor.extract(ctx);

    if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
      return SendError.missingSessionId(ctx);
    }

    String sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
    McpStreamableServerSession session = this.sessions.get(sessionId);

    if (session == null) {
      return SendError.sessionNotFound(ctx, sessionId);
    }

    LOG.debug("Handling GET request for session: {}", sessionId);

    try {
      ctx.setResponseType(TEXT_EVENT_STREAM);
      return ctx.upgrade(
          sse -> {
            sse.onClose(
                () -> LOG.debug("SSE connection closed by client for session: {}", sessionId));

            var sessionTransport = new JoobyStreamableMcpSessionTransport(sessionId, sse);

            // Check if this is a replay request
            if (ctx.header(HttpHeaders.LAST_EVENT_ID).isPresent()) {
              String lastId = ctx.header(HttpHeaders.LAST_EVENT_ID).value();

              try {
                session
                    .replay(lastId)
                    .contextWrite(
                        reactorCtx ->
                            reactorCtx
                                .put(McpTransportContext.KEY, transportContext)
                                .put("CTX", ctx))
                    .toIterable()
                    .forEach(
                        message -> {
                          try {
                            sessionTransport
                                .sendMessage(message)
                                .contextWrite(
                                    reactorCtx ->
                                        reactorCtx.put(McpTransportContext.KEY, transportContext))
                                .block();
                          } catch (Exception e) {
                            LOG.error("Failed to replay message: {}", e.getMessage());
                            sse.send(SSE_ERROR_EVENT, e.getMessage());
                          }
                        });
              } catch (Exception e) {
                LOG.error("Failed to replay messages: {}", e.getMessage());
                sse.send(SSE_ERROR_EVENT, e.getMessage());
              }
            } else {
              // Establish new listening stream
              McpStreamableServerSession.McpStreamableServerSessionStream listeningStream =
                  session.listeningStream(sessionTransport);

              sse.onClose(
                  () -> {
                    LOG.debug("SSE connection has been closed for session: {}", sessionId);
                    listeningStream.close();
                  });
            }
          });
    } catch (Exception e) {
      LOG.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
      return SendError.internalError(ctx, sessionId);
    }
  }

  /**
   * Handles POST requests for incoming JSON-RPC messages from clients.
   *
   * @param ctx The Jooby context for the incoming request
   */
  private Object handlePost(Context ctx) {
    if (this.isClosing) {
      return SendError.serverIsShuttingDown(ctx);
    }

    if (!ctx.accept(TEXT_EVENT_STREAM) || !ctx.accept(MediaType.json)) {
      return SendError.invalidAcceptHeader(ctx, List.of(TEXT_EVENT_STREAM, MediaType.json));
    }

    McpTransportContext transportContext = this.contextExtractor.extract(ctx);
    String sessionId = null;

    try {
      var body = ctx.body().valueOrNull();
      if (body == null) {
        return SendError.error(
            ctx, StatusCode.BAD_REQUEST, INVALID_REQUEST, "Request body is missing");
      }
      McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, body);

      // Handle initialization request
      if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
          && McpSchema.METHOD_INITIALIZE.equals(jsonrpcRequest.method())) {

        McpSchema.InitializeRequest initRequest =
            mcpJsonMapper.convertValue(jsonrpcRequest.params(), McpSchema.InitializeRequest.class);
        McpStreamableServerSession.McpStreamableServerSessionInit initObj =
            this.sessionFactory.startSession(initRequest);
        sessionId = initObj.session().getId();
        this.sessions.put(sessionId, initObj.session());

        try {
          McpSchema.InitializeResult initResult = initObj.initResult().block();

          ctx.setResponseHeader(HttpHeaders.MCP_SESSION_ID, sessionId);
          return new McpSchema.JSONRPCResponse(
              McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult, null);
        } catch (Exception e) {
          LOG.error("Failed to initialize session: {}", e.getMessage());
          return SendError.internalError(ctx, sessionId);
        }
      }

      // Handle other messages that require a session
      if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
        return SendError.missingSessionId(ctx);
      }

      sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
      McpStreamableServerSession session = this.sessions.get(sessionId);

      if (session == null) {
        return SendError.sessionNotFound(ctx, sessionId);
      }

      if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
        session
            .accept(jsonrpcResponse)
            .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
            .block();
        return StatusCode.ACCEPTED;
      } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
        session
            .accept(jsonrpcNotification)
            .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
            .block();
        return StatusCode.ACCEPTED;
      } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
        ctx.setResponseType(TEXT_EVENT_STREAM);

        String finalSessionId = sessionId;
        return ctx.upgrade(
            sse -> {
              sse.onClose(
                  () ->
                      LOG.debug(
                          "Request response stream completed for session: {}", finalSessionId));

              JoobyStreamableMcpSessionTransport sessionTransport =
                  new JoobyStreamableMcpSessionTransport(finalSessionId, sse);

              try {
                session
                    .responseStream(jsonrpcRequest, sessionTransport)
                    .contextWrite(
                        reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                    .block();
              } catch (Exception e) {
                LOG.error("Failed to handle request stream: {}", e.getMessage());
                sse.send(SSE_ERROR_EVENT, e.getMessage());
              }
            });
      } else {
        return SendError.unknownMsgType(ctx, sessionId);
      }
    } catch (IllegalArgumentException | IOException e) {
      LOG.error("Failed to deserialize message: {}", e.getMessage());
      return SendError.msgParseError(ctx, sessionId);
    } catch (Exception e) {
      LOG.error("Unexpected error occurred while handling message: {}", e.getMessage());
      return SendError.internalError(ctx, sessionId);
    }
  }

  /**
   * Handles DELETE requests for session deletion.
   *
   * @param ctx The Jooby context for the incoming request
   * @return A ServerResponse indicating success or appropriate error status
   */
  private Object handleDelete(Context ctx) {
    if (this.isClosing) {
      return SendError.serverIsShuttingDown(ctx);
    }

    if (this.disallowDelete) {
      return SendError.deletionNotAllowed(ctx);
    }

    McpTransportContext transportContext = this.contextExtractor.extract(ctx);

    if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
      return SendError.missingSessionId(ctx);
    }

    String sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
    McpStreamableServerSession session = this.sessions.get(sessionId);

    if (session == null) {
      return SendError.sessionNotFound(ctx, sessionId);
    }

    try {
      session
          .delete()
          .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
          .block();
      this.sessions.remove(sessionId);
      return StatusCode.NO_CONTENT;
    } catch (Exception e) {
      LOG.error("Failed to delete session {}: {}", sessionId, e.getMessage());
      return SendError.internalError(ctx, sessionId);
    }
  }

  @Override
  public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (this.sessions.isEmpty()) {
      LOG.debug("No active sessions to broadcast message to");
      return Mono.empty();
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
    }

    return Mono.fromRunnable(
        () -> {
          this.sessions.values().parallelStream()
              .forEach(
                  session -> {
                    try {
                      session.sendNotification(method, params).block();
                    } catch (Exception e) {
                      LOG.error(
                          "Failed to send message to session {}: {}",
                          session.getId(),
                          e.getMessage());
                    }
                  });
        });
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Mono.fromRunnable(
            () -> {
              this.isClosing = true;
              if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "Initiating graceful shutdown with {} active sessions", this.sessions.size());
              }

              this.sessions.values().parallelStream()
                  .forEach(
                      session -> {
                        try {
                          session.closeGracefully().block();
                        } catch (Exception e) {
                          LOG.error(
                              "Failed to close session {}: {}", session.getId(), e.getMessage());
                        }
                      });

              this.sessions.clear();
              LOG.debug("Graceful shutdown completed");
            })
        .then()
        .doOnSuccess(
            v -> {
              if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
              }
            });
  }

  private class JoobyStreamableMcpSessionTransport implements McpStreamableServerTransport {

    private final String sessionId;
    private final ServerSentEmitter sse;
    private volatile boolean closed = false;

    JoobyStreamableMcpSessionTransport(String sessionId, ServerSentEmitter sse) {
      this.sessionId = sessionId;
      this.sse = sse;
      LOG.debug("Streamable session transport {} initialized with SSE", sessionId);
    }

    /**
     * Sends a JSON-RPC message to the client through the SSE connection.
     *
     * @param message The JSON-RPC message to send
     * @return A Mono that completes when the message has been sent
     */
    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
      return sendMessage(message, null);
    }

    /**
     * Sends a JSON-RPC message to the client through the SSE connection with a specific message ID.
     *
     * @param message The JSON-RPC message to send
     * @param messageId The message ID for SSE event identification
     * @return A Mono that completes when the message has been sent
     */
    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
      return Mono.fromRunnable(
          () -> {
            try {
              if (this.closed) {
                LOG.debug("Session {} was closed during message send attempt", this.sessionId);
                return;
              }

              String jsonText = mcpJsonMapper.writeValueAsString(message);
              sse.send(
                  new ServerSentMessage(jsonText)
                      .setId(messageId != null ? messageId : this.sessionId)
                      .setEvent(MESSAGE_EVENT_TYPE));
              LOG.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
            } catch (Exception e) {
              LOG.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
              try {
                sse.send(SSE_ERROR_EVENT, e.getMessage());
              } catch (Exception errorEx) {
                LOG.error(
                    "Failed to send error to SSE session {}: {}",
                    this.sessionId,
                    errorEx.getMessage());
              }
            }
          });
    }

    /**
     * Converts data from one type to another using the configured McpJsonMapper.
     *
     * @param data The source data object to convert
     * @param typeRef The target type reference
     * @param <T> The target type
     * @return The converted object of type T
     */
    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
      return mcpJsonMapper.convertValue(data, typeRef);
    }

    /**
     * Initiates a graceful shutdown of the transport.
     *
     * @return A Mono that completes when the shutdown is complete
     */
    @Override
    public Mono<Void> closeGracefully() {
      return Mono.fromRunnable(this::close);
    }

    /** Closes the transport immediately. */
    @Override
    public void close() {
      try {
        if (this.closed) {
          LOG.debug("Session transport {} already closed", this.sessionId);
          return;
        }

        this.closed = true;
        sse.close();
        LOG.debug("Successfully closed SSE session {}", sessionId);
      } catch (Exception e) {
        LOG.warn("Failed to close SSE session {}: {}", sessionId, e.getMessage());
      }
    }
  }
}
