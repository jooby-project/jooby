/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import static io.jooby.mcp.transport.TransportConstants.*;

import java.io.IOException;

import io.jooby.*;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import reactor.core.publisher.Mono;

public class SseTransportProvider extends AbstractMcpTransportProvider {

  private static final String ENDPOINT_EVENT_TYPE = "endpoint";
  private static final String SESSION_ID_KEY = "sessionId";
  private final String messageEndpoint;

  public SseTransportProvider(
      Jooby app,
      McpServerConfig serverConfig,
      McpJsonMapper mcpJsonMapper,
      McpTransportContextExtractor<Context> contextExtractor) {
    super(mcpJsonMapper, contextExtractor);
    this.messageEndpoint = serverConfig.getMessageEndpoint();
    String sseEndpoint = serverConfig.getSseEndpoint();

    app.head(sseEndpoint, ctx -> StatusCode.OK).produces(TEXT_EVENT_STREAM);
    app.sse(sseEndpoint, this::handleSseConnection);
    app.post(this.messageEndpoint, this::handleMessage);
  }

  @Override
  protected String transportName() {
    return "SSE";
  }

  private void handleSseConnection(ServerSentEmitter sse) {
    JoobyMcpSessionTransport transport = new JoobyMcpSessionTransport(mcpJsonMapper, sse);
    McpServerSession session = sessionFactory.create(transport);
    String sessionId = session.getId();

    log.debug("New SSE connection established. Session ID: {}", sessionId);
    sessions.put(sessionId, session);

    sse.onClose(
        () -> {
          log.debug("Session with ID {} has been cancelled", sessionId);
          sessions.remove(sessionId);
        });

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
          .message("Session ID missing")
          .build();
    }

    String sessionId = ctx.query(SESSION_ID_KEY).value();
    McpServerSession session = sessions.get(sessionId);

    if (session == null) {
      ctx.setResponseCode(StatusCode.NOT_FOUND);
      return McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
          .message("Session not found")
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
                  reactorCtx.put(McpTransportContext.KEY, transportContext).put("CTX", ctx))
          .then(Mono.just((Object) StatusCode.OK))
          .onErrorResume(
              error -> {
                log.error("Error processing message: {}", error.getMessage());
                return Mono.just(StatusCode.OK);
              })
          .switchIfEmpty(Mono.just((Object) StatusCode.OK))
          .block();
    } catch (IOException | IllegalArgumentException e) {
      log.error("Failed to deserialize message: {}", e.getMessage());
      return McpError.builder(McpSchema.ErrorCodes.PARSE_ERROR)
          .message("Invalid message format")
          .build();
    }
  }

  private static class JoobyMcpSessionTransport extends AbstractMcpTransport {
    private final ServerSentEmitter sse;

    public JoobyMcpSessionTransport(McpJsonMapper mcpJsonMapper, ServerSentEmitter sse) {
      super(mcpJsonMapper);
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
              log.error("Failed to send message: {}", e.getMessage());
              sse.send(SSE_ERROR_EVENT, e.getMessage());
            }
          });
    }

    @Override
    public void close() {
      sse.close();
    }
  }
}
