/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static io.jooby.internal.mcp.transport.TransportConstants.TEXT_EVENT_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class StatelessTransportProviderTest {

  @Mock Jooby app;
  @Mock McpJsonMapper jsonMapper;
  @Mock McpServerConfig serverConfig;
  @Mock McpTransportContextExtractor<Context> contextExtractor;
  @Mock McpTransportContext transportContext;
  @Mock McpStatelessServerHandler mcpHandler;
  @Mock Context ctx;

  private StatelessTransportProvider provider;
  private Route.Handler headHandler;
  private Route.Handler getHandler;
  private Route.Handler postHandler;

  @BeforeEach
  void setup() {
    lenient().when(serverConfig.getMcpEndpoint()).thenReturn("/mcp");

    // Boilerplate to prevent NPEs when SendError attempts to format and send error responses
    lenient().when(ctx.accept(MediaType.json)).thenReturn(true);
    lenient().when(ctx.render(any())).thenReturn(ctx);
    lenient().when(ctx.setResponseCode(any())).thenReturn(ctx);

    // Prevent Reactor NPEs when setting contextual data
    lenient().when(contextExtractor.extract(any())).thenReturn(transportContext);

    Route headRoute = mock(Route.class);
    lenient().when(headRoute.produces(any())).thenReturn(headRoute);
    lenient().when(app.head(anyString(), any())).thenReturn(headRoute);

    ArgumentCaptor<Route.Handler> headCap = ArgumentCaptor.forClass(Route.Handler.class);
    ArgumentCaptor<Route.Handler> getCap = ArgumentCaptor.forClass(Route.Handler.class);
    ArgumentCaptor<Route.Handler> postCap = ArgumentCaptor.forClass(Route.Handler.class);

    provider = new StatelessTransportProvider(app, jsonMapper, serverConfig, contextExtractor);
    provider.setMcpHandler(mcpHandler);

    verify(app).head(eq("/mcp"), headCap.capture());
    verify(app).get(eq("/mcp"), getCap.capture());
    verify(app).post(eq("/mcp"), postCap.capture());

    headHandler = headCap.getValue();
    getHandler = getCap.getValue();
    postHandler = postCap.getValue();
  }

  // --- HEAD & GET ROUTES ---

  @Test
  void testHeadHandler() throws Exception {
    Object result = headHandler.apply(ctx);
    assertEquals(StatusCode.OK, result);
  }

  @Test
  void testGetHandler() throws Exception {
    getHandler.apply(ctx);
    verify(ctx).setResponseCode(StatusCode.METHOD_NOT_ALLOWED);
  }

  // --- POST ROUTE: EARLY EXITS ---

  @Test
  void testPost_IsClosing() throws Exception {
    provider.closeGracefully().block();
    postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
  }

  @Test
  void testPost_InvalidAccept() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(false);

    postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
  }

  @Test
  void testPost_BodyMissing() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn(null);

    postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
  }

  // --- POST ROUTE: DESERIALIZATION FAILS ---

  @Test
  void testPost_IllegalArgumentException_DuringDeserialization() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("invalid-body");

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "invalid-body"))
          .thenThrow(new IllegalArgumentException("Format Invalid"));

      postHandler.apply(ctx);

      verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    }
  }

  @Test
  void testPost_GenericException_DuringProcessing() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);

    // Throw an unhandled exception inside the try block to trigger the generic Exception catch
    when(ctx.body()).thenThrow(new RuntimeException("Unexpected I/O failure"));

    postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
  }

  // --- POST ROUTE: SUCCESSFUL MESSAGE PARSING ---

  @Test
  void testPost_JSONRPCRequest_Success() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCRequest req = mock(McpSchema.JSONRPCRequest.class);
    McpSchema.JSONRPCResponse expectedResponse = mock(McpSchema.JSONRPCResponse.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(req);
      when(mcpHandler.handleRequest(transportContext, req)).thenReturn(Mono.just(expectedResponse));

      Object actualResponse = postHandler.apply(ctx);

      assertEquals(expectedResponse, actualResponse);
    }
  }

  @Test
  void testPost_JSONRPCRequest_HandlerThrowsException() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCRequest req = mock(McpSchema.JSONRPCRequest.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(req);
      when(mcpHandler.handleRequest(transportContext, req))
          .thenReturn(Mono.error(new RuntimeException("Handler crashed")));

      postHandler.apply(ctx);

      verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
    }
  }

  @Test
  void testPost_JSONRPCNotification_Success() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(notif);
      when(mcpHandler.handleNotification(transportContext, notif)).thenReturn(Mono.empty());

      Object actualResponse = postHandler.apply(ctx);

      assertEquals(StatusCode.ACCEPTED, actualResponse);
    }
  }

  @Test
  void testPost_JSONRPCNotification_HandlerThrowsException() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(notif);
      when(mcpHandler.handleNotification(transportContext, notif))
          .thenReturn(Mono.error(new RuntimeException("Handler crashed")));

      postHandler.apply(ctx);

      verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
    }
  }

  @Test
  void testPost_UnknownMessageType() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    // JSONRPCResponse is not an allowed inbound request type for the stateless provider
    McpSchema.JSONRPCResponse unknown = mock(McpSchema.JSONRPCResponse.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body"))
          .thenReturn(unknown);

      postHandler.apply(ctx);

      verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    }
  }
}
