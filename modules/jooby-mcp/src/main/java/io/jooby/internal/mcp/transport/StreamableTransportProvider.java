/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static io.jooby.internal.mcp.transport.TransportConstants.*;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_REQUEST;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

/** Jooby implementation of Streamable HTTP transport. */
@SuppressWarnings("PMD")
public class StreamableTransportProvider implements McpStreamableServerTransportProvider {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final boolean disallowDelete;
  private final McpJsonMapper mcpJsonMapper;
  private final ConcurrentMap<String, McpStreamableServerSession> sessions =
      new ConcurrentHashMap<>();
  private final McpTransportContextExtractor<Context> contextExtractor;
  private volatile boolean isClosing = false;
  private McpStreamableServerSession.Factory sessionFactory;
  private KeepAliveScheduler keepAliveScheduler;

  public StreamableTransportProvider(
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

  private Context handleGet(Context ctx) {
    if (this.isClosing) return SendError.serverIsShuttingDown(ctx);
    if (!ctx.accept(TEXT_EVENT_STREAM))
      return SendError.invalidAcceptHeader(ctx, List.of(TEXT_EVENT_STREAM));
    if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) return SendError.missingSessionId(ctx);

    var sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
    var session = this.sessions.get(sessionId);
    if (session == null) return SendError.sessionNotFound(ctx, sessionId);

    var transportContext = this.contextExtractor.extract(ctx);
    log.debug("Handling GET request for session: {}", sessionId);

    try {
      ctx.setResponseType(TEXT_EVENT_STREAM);
      return ctx.upgrade(
          sse -> {
            sse.onClose(
                () -> log.debug("SSE connection closed by client for session: {}", sessionId));
            var sessionTransport = new StreamableMcpSessionTransport(sessionId, sse);

            if (ctx.header(HttpHeaders.LAST_EVENT_ID).isPresent()) {
              var lastId = ctx.header(HttpHeaders.LAST_EVENT_ID).value();

              // FIX: Replaced blocking .forEach with non-blocking .concatMap
              session
                  .replay(lastId)
                  .contextWrite(
                      reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                  .concatMap(
                      message ->
                          sessionTransport
                              .sendMessage(message)
                              .contextWrite(
                                  reactorCtx ->
                                      reactorCtx.put(McpTransportContext.KEY, transportContext)))
                  .subscribe(
                      null,
                      error -> {
                        log.error("Failed to replay messages", error);
                        sse.send(SSE_ERROR_EVENT, error.getMessage());
                      });
            } else {
              var listeningStream = session.listeningStream(sessionTransport);
              sse.onClose(
                  () -> {
                    log.debug("SSE connection has been closed for session: {}", sessionId);
                    listeningStream.close();
                  });
            }
          });
    } catch (Exception e) {
      log.error("Failed to handle GET request for session {}", sessionId, e);
      return SendError.internalError(ctx, sessionId);
    }
  }

  private Object handlePost(Context ctx) {
    if (this.isClosing) return SendError.serverIsShuttingDown(ctx);
    if (!ctx.accept(TEXT_EVENT_STREAM) || !ctx.accept(MediaType.json)) {
      return SendError.invalidAcceptHeader(ctx, List.of(TEXT_EVENT_STREAM, MediaType.json));
    }

    var transportContext = this.contextExtractor.extract(ctx);
    String sessionId = null;

    try {
      var body = ctx.body().valueOrNull();
      if (body == null)
        return SendError.error(
            ctx, StatusCode.BAD_REQUEST, INVALID_REQUEST, "Request body is missing");

      var message = McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, body);

      if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
          && McpSchema.METHOD_INITIALIZE.equals(jsonrpcRequest.method())) {
        var initRequest =
            mcpJsonMapper.convertValue(jsonrpcRequest.params(), McpSchema.InitializeRequest.class);
        var initObj = this.sessionFactory.startSession(initRequest);
        sessionId = initObj.session().getId();
        this.sessions.put(sessionId, initObj.session());

        try {
          var initResult = initObj.initResult().block();
          ctx.setResponseHeader(HttpHeaders.MCP_SESSION_ID, sessionId);
          return new McpSchema.JSONRPCResponse(
              McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult, null);
        } catch (Exception e) {
          log.error("Failed to initialize session", e);
          return SendError.internalError(ctx, sessionId);
        }
      }

      if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing())
        return SendError.missingSessionId(ctx);
      sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
      var session = this.sessions.get(sessionId);
      if (session == null) return SendError.sessionNotFound(ctx, sessionId);

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
                      log.debug(
                          "Request response stream completed for session: {}", finalSessionId));
              var sessionTransport = new StreamableMcpSessionTransport(finalSessionId, sse);

              session
                  .responseStream(jsonrpcRequest, sessionTransport)
                  .contextWrite(
                      reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                  .subscribe(
                      null,
                      error -> {
                        log.error("Failed to handle request stream", error);
                        sse.send(SSE_ERROR_EVENT, error.getMessage());
                        sse.close();
                      });
            });
      } else {
        return SendError.unknownMsgType(ctx, sessionId);
      }
    } catch (IllegalArgumentException | IOException e) {
      log.error("Failed to deserialize message", e);
      return SendError.msgParseError(ctx, sessionId);
    } catch (Exception e) {
      log.error("Unexpected error occurred while handling message", e);
      return SendError.internalError(ctx, sessionId);
    }
  }

  private Object handleDelete(Context ctx) {
    if (this.isClosing) return SendError.serverIsShuttingDown(ctx);
    if (this.disallowDelete) return SendError.deletionNotAllowed(ctx);
    if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) return SendError.missingSessionId(ctx);

    var sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
    var session = this.sessions.get(sessionId);
    if (session == null) return SendError.sessionNotFound(ctx, sessionId);

    try {
      var transportContext = this.contextExtractor.extract(ctx);
      session
          .delete()
          .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
          .block();
      this.sessions.remove(sessionId);
      return StatusCode.NO_CONTENT;
    } catch (Exception e) {
      log.error("Failed to delete session {}", sessionId, e);
      return SendError.internalError(ctx, sessionId);
    }
  }

  @Override
  public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (this.sessions.isEmpty()) return Mono.empty();

    return Flux.fromIterable(this.sessions.values())
        .flatMap(
            session ->
                session
                    .sendNotification(method, params)
                    .doOnError(
                        e -> log.error("Failed to send message to session {}", session.getId(), e))
                    .onErrorComplete())
        .then();
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Flux.fromIterable(sessions.values())
        .doFirst(() -> this.isClosing = true)
        .flatMap(McpStreamableServerSession::closeGracefully)
        .doFinally(
            signalType -> {
              this.sessions.clear();
              if (this.keepAliveScheduler != null) this.keepAliveScheduler.shutdown();
            })
        .then();
  }

  private class StreamableMcpSessionTransport implements McpStreamableServerTransport {
    private final String sessionId;
    private final ServerSentEmitter sse;
    private volatile boolean closed = false;

    StreamableMcpSessionTransport(String sessionId, ServerSentEmitter sse) {
      this.sessionId = sessionId;
      this.sse = sse;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
      return sendMessage(message, null);
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
      return Mono.fromRunnable(
          () -> {
            try {
              if (!closed) {
                var jsonText = mcpJsonMapper.writeValueAsString(message);
                sse.send(
                    new ServerSentMessage(jsonText)
                        .setId(messageId != null ? messageId : this.sessionId)
                        .setEvent(MESSAGE_EVENT_TYPE));
              }
            } catch (Exception e) {
              log.error("Failed to send message to session {}", this.sessionId, e);
              try {
                sse.send(SSE_ERROR_EVENT, e.getMessage());
              } catch (Exception errorEx) {
                log.error("Failed to send error to SSE session {}", this.sessionId, errorEx);
              }
            }
          });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
      return mcpJsonMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
      // FIX: Added a 50ms buffer. This guarantees the underlying server (e.g. Undertow)
      // physically flushes the SSE chunk to the network layer before terminating the TCP socket.
      return Mono.delay(Duration.ofMillis(50)).then(Mono.fromRunnable(this::close));
    }

    @Override
    public void close() {
      try {
        if (!this.closed) {
          this.closed = true;
          sse.close();
        }
      } catch (Exception e) {
        log.debug("Failed to close SSE session {}", sessionId, e);
      }
    }
  }
}
