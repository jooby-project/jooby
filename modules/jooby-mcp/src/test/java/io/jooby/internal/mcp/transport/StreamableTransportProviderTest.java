/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static io.jooby.internal.mcp.transport.TransportConstants.SSE_ERROR_EVENT;
import static io.jooby.internal.mcp.transport.TransportConstants.TEXT_EVENT_STREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.*;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.value.Value;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class StreamableTransportProviderTest {

  @Mock Jooby app;
  @Mock McpJsonMapper jsonMapper;
  @Mock McpServerConfig serverConfig;
  @Mock McpTransportContextExtractor<Context> contextExtractor;
  @Mock McpTransportContext transportContext;
  @Mock Context ctx;
  @Mock McpStreamableServerSession session;
  @Mock McpStreamableServerSession.Factory sessionFactory;
  @Mock ServerSentEmitter sse;

  private StreamableTransportProvider provider;
  private Route.Handler getHandler;
  private Route.Handler postHandler;
  private Route.Handler deleteHandler;

  @BeforeEach
  void setup() {
    lenient().when(serverConfig.getMcpEndpoint()).thenReturn("/mcp");
    lenient().when(serverConfig.isDisallowDelete()).thenReturn(false);
    lenient().when(serverConfig.getKeepAliveInterval()).thenReturn(null);
    lenient().when(contextExtractor.extract(any())).thenReturn(transportContext);

    Route headRoute = mock(Route.class);
    lenient().when(headRoute.produces(any(MediaType.class))).thenReturn(headRoute);
    lenient().when(headRoute.produces(any())).thenReturn(headRoute);
    lenient().when(app.head(anyString(), any())).thenReturn(headRoute);

    ArgumentCaptor<Route.Handler> getCap = ArgumentCaptor.forClass(Route.Handler.class);
    ArgumentCaptor<Route.Handler> postCap = ArgumentCaptor.forClass(Route.Handler.class);
    ArgumentCaptor<Route.Handler> deleteCap = ArgumentCaptor.forClass(Route.Handler.class);

    provider = new StreamableTransportProvider(app, jsonMapper, serverConfig, contextExtractor);
    provider.setSessionFactory(sessionFactory);

    verify(app).get(eq("/mcp"), getCap.capture());
    verify(app).post(eq("/mcp"), postCap.capture());
    verify(app).delete(eq("/mcp"), deleteCap.capture());

    getHandler = getCap.getValue();
    postHandler = postCap.getValue();
    deleteHandler = deleteCap.getValue();
  }

  private void injectSession(String id, McpStreamableServerSession sess) throws Exception {
    Field field = StreamableTransportProvider.class.getDeclaredField("sessions");
    field.setAccessible(true);
    ((ConcurrentMap) field.get(provider)).put(id, sess);
  }

  // --- CONSTRUCTOR / KEEP ALIVE ---

  @Test
  void testConstructorWithKeepAlive() {
    when(serverConfig.getKeepAliveInterval()).thenReturn(30);
    KeepAliveScheduler.Builder builderMock = mock(KeepAliveScheduler.Builder.class);
    KeepAliveScheduler schedulerMock = mock(KeepAliveScheduler.class);

    try (MockedStatic<KeepAliveScheduler> schedulerStatic = mockStatic(KeepAliveScheduler.class)) {
      schedulerStatic.when(() -> KeepAliveScheduler.builder(any())).thenReturn(builderMock);
      when(builderMock.initialDelay(any(Duration.class))).thenReturn(builderMock);
      when(builderMock.interval(any(Duration.class))).thenReturn(builderMock);
      when(builderMock.build()).thenReturn(schedulerMock);

      StreamableTransportProvider prov =
          new StreamableTransportProvider(app, jsonMapper, serverConfig, contextExtractor);

      verify(schedulerMock).start();
      prov.closeGracefully().block();
      verify(schedulerMock).shutdown();
    }
  }

  // --- GET ROUTE TESTS ---

  @Test
  void testGet_IsClosing() throws Exception {
    provider.closeGracefully().block();
    getHandler.apply(ctx); // Triggers SendError branch
  }

  @Test
  void testGet_InvalidAccept() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(false);
    getHandler.apply(ctx);
  }

  @Test
  void testGet_MissingSessionId() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    var val = mock(Value.class);
    when(val.isMissing()).thenReturn(true);
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);
    getHandler.apply(ctx);
  }

  @Test
  void testGet_SessionNotFound() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("missing-id");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);
    getHandler.apply(ctx);
  }

  @Test
  void testGet_ThrowsException_ReturnsInternalError() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);

    injectSession("sess-1", session);

    // Throw inside the try-block (ctx.upgrade) to ensure the catch block handles it gracefully
    when(ctx.upgrade(any(ServerSentEmitter.Handler.class)))
        .thenThrow(new RuntimeException("Simulated framework upgrade failure"));

    getHandler.apply(ctx);
  }

  @Test
  void testGet_Success_ListeningStream_And_TransportMethods() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    Value lastEventVal = mock(Value.class);
    when(lastEventVal.isPresent()).thenReturn(false);
    when(ctx.header(HttpHeaders.LAST_EVENT_ID)).thenReturn(lastEventVal);

    injectSession("sess-1", session);

    when(ctx.upgrade(any(ServerSentEmitter.Handler.class)))
        .thenAnswer(
            inv -> {
              ServerSentEmitter.Handler h = inv.getArgument(0);
              h.handle(sse);
              return ctx;
            });

    McpStreamableServerSession.McpStreamableServerSessionStream listeningStream =
        mock(McpStreamableServerSession.McpStreamableServerSessionStream.class);
    when(session.listeningStream(any())).thenReturn(listeningStream);

    getHandler.apply(ctx);

    ArgumentCaptor<McpStreamableServerTransport> transportCap =
        ArgumentCaptor.forClass(McpStreamableServerTransport.class);
    verify(session).listeningStream(transportCap.capture());
    McpStreamableServerTransport transport = transportCap.getValue();

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    when(jsonMapper.writeValueAsString(msg)).thenReturn("{\"json\":\"1\"}");

    // Test 1: transport.sendMessage (Success, defaults to sessionId)
    transport.sendMessage(msg).block();
    ArgumentCaptor<ServerSentMessage> cap1 = ArgumentCaptor.forClass(ServerSentMessage.class);
    verify(sse).send(cap1.capture());
    assertEquals("{\"json\":\"1\"}", cap1.getValue().getData());
    assertEquals("sess-1", cap1.getValue().getId()); // Verifies it defaulted to session ID

    // Test 2: transport.sendMessage with explicitly defined ID
    transport.sendMessage(msg, "custom-id").block();
    ArgumentCaptor<ServerSentMessage> cap2 = ArgumentCaptor.forClass(ServerSentMessage.class);
    verify(sse, times(2)).send(cap2.capture());
    // With a fresh captor, Index 0 is the first call, Index 1 is the second call.
    assertEquals("custom-id", cap2.getAllValues().get(1).getId());

    // Test 3: transport.sendMessage (Exception)
    when(jsonMapper.writeValueAsString(msg)).thenThrow(new RuntimeException("JSON error"));
    transport.sendMessage(msg).block();
    verify(sse).send(eq(SSE_ERROR_EVENT), eq("JSON error"));

    // Test 4: transport.sendMessage (Double Exception)
    doThrow(new RuntimeException("Double Exception"))
        .when(sse)
        .send(eq(SSE_ERROR_EVENT), anyString());
    transport.sendMessage(msg).block(); // Should catch and not propagate

    // Test 5: unmarshalFrom
    TypeRef<String> ref = new TypeRef<>() {};
    when(jsonMapper.convertValue("data", ref)).thenReturn("data");
    assertEquals("data", transport.unmarshalFrom("data", ref));

    // Test 6: closeGracefully
    transport.closeGracefully().block();
    verify(sse).close();

    ArgumentCaptor<SneakyThrows.Runnable> onCloseCap =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(sse).onClose(onCloseCap.capture());
    onCloseCap.getValue().run();
    verify(listeningStream).close();
  }

  @Test
  void testGet_Success_Replay_SubscriptionError() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    Value lastEventVal = mock(Value.class);
    when(lastEventVal.isPresent()).thenReturn(true);
    when(lastEventVal.value()).thenReturn("last-1");
    when(ctx.header(HttpHeaders.LAST_EVENT_ID)).thenReturn(lastEventVal);

    injectSession("sess-1", session);

    when(ctx.upgrade(any(ServerSentEmitter.Handler.class)))
        .thenAnswer(
            inv -> {
              ServerSentEmitter.Handler h = inv.getArgument(0);
              h.handle(sse);
              return ctx;
            });

    when(session.replay("last-1")).thenReturn(Flux.error(new RuntimeException("Replay Error")));
    getHandler.apply(ctx);
    verify(sse).send(eq(SSE_ERROR_EVENT), eq("Replay Error"));
  }

  @Test
  void testGet_Success_Replay_Success() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    Value lastEventVal = mock(Value.class);
    when(lastEventVal.isPresent()).thenReturn(true);
    when(lastEventVal.value()).thenReturn("last-1");
    when(ctx.header(HttpHeaders.LAST_EVENT_ID)).thenReturn(lastEventVal);

    injectSession("sess-1", session);

    when(ctx.upgrade(any(ServerSentEmitter.Handler.class)))
        .thenAnswer(
            inv -> {
              ServerSentEmitter.Handler h = inv.getArgument(0);
              h.handle(sse);
              return ctx;
            });

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    when(session.replay("last-1")).thenReturn(Flux.just(msg));
    when(jsonMapper.writeValueAsString(msg)).thenReturn("{\"msg\":\"1\"}");

    getHandler.apply(ctx);

    ArgumentCaptor<ServerSentMessage> sseMsgCap = ArgumentCaptor.forClass(ServerSentMessage.class);
    verify(sse).send(sseMsgCap.capture());
    assertEquals("{\"msg\":\"1\"}", sseMsgCap.getValue().getData());
  }

  // --- POST ROUTE TESTS ---

  @Test
  void testPost_IsClosing() throws Exception {
    provider.closeGracefully().block();
    postHandler.apply(ctx);
  }

  @Test
  void testPost_InvalidAccept() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(false);
    postHandler.apply(ctx);
  }

  @Test
  void testPost_BodyMissing() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn(null);
    postHandler.apply(ctx);
  }

  @Test
  void testPost_IllegalArgumentException() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body"))
          .thenThrow(new IllegalArgumentException("Format Invalid"));
      postHandler.apply(ctx);
    }
  }

  @Test
  void testPost_Initialize_Success() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCRequest req = mock(McpSchema.JSONRPCRequest.class);
    when(req.method()).thenReturn(McpSchema.METHOD_INITIALIZE);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(req);
      McpSchema.InitializeRequest initReq = mock(McpSchema.InitializeRequest.class);
      when(jsonMapper.convertValue(any(), eq(McpSchema.InitializeRequest.class)))
          .thenReturn(initReq);

      McpStreamableServerSession.McpStreamableServerSessionInit initObj =
          mock(McpStreamableServerSession.McpStreamableServerSessionInit.class);
      when(sessionFactory.startSession(initReq)).thenReturn(initObj);
      when(initObj.session()).thenReturn(session);
      when(session.getId()).thenReturn("sess-init");
      McpSchema.InitializeResult initRes = mock(McpSchema.InitializeResult.class);
      when(initObj.initResult()).thenReturn(Mono.just(initRes));

      Object res = postHandler.apply(ctx);

      assertTrue(res instanceof McpSchema.JSONRPCResponse);
      verify(ctx).setResponseHeader(HttpHeaders.MCP_SESSION_ID, "sess-init");
    }
  }

  @Test
  void testPost_Initialize_Exception() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCRequest req = mock(McpSchema.JSONRPCRequest.class);
    when(req.method()).thenReturn(McpSchema.METHOD_INITIALIZE);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(req);
      McpSchema.InitializeRequest initReq = mock(McpSchema.InitializeRequest.class);
      when(jsonMapper.convertValue(any(), eq(McpSchema.InitializeRequest.class)))
          .thenReturn(initReq);
      when(sessionFactory.startSession(initReq)).thenThrow(new RuntimeException("Crash"));

      postHandler.apply(ctx);
    }
  }

  @Test
  void testPost_MissingSessionId() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);
    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(true);
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(notif);
      postHandler.apply(ctx);
    }
  }

  @Test
  void testPost_JSONRPCResponse() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    injectSession("sess-1", session);

    McpSchema.JSONRPCResponse resp = mock(McpSchema.JSONRPCResponse.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(resp);
      when(session.accept(resp)).thenReturn(Mono.empty());

      Object res = postHandler.apply(ctx);
      assertEquals(StatusCode.ACCEPTED, res);
      verify(session).accept(resp);
    }
  }

  @Test
  void testPost_JSONRPCNotification() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    injectSession("sess-1", session);

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(notif);
      when(session.accept(notif)).thenReturn(Mono.empty());

      Object res = postHandler.apply(ctx);
      assertEquals(StatusCode.ACCEPTED, res);
      verify(session).accept(notif);
    }
  }

  @Test
  void testPost_JSONRPCRequest_StreamError() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    injectSession("sess-1", session);

    McpSchema.JSONRPCRequest req = mock(McpSchema.JSONRPCRequest.class);
    when(req.method()).thenReturn("customMethod");

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(req);

      when(ctx.upgrade(any(ServerSentEmitter.Handler.class)))
          .thenAnswer(
              inv -> {
                ServerSentEmitter.Handler h = inv.getArgument(0);
                h.handle(sse);
                return ctx;
              });

      when(session.responseStream(eq(req), any()))
          .thenReturn(Mono.error(new RuntimeException("Stream Error")));

      postHandler.apply(ctx);

      verify(sse).send(eq(SSE_ERROR_EVENT), eq("Stream Error"));
      verify(sse).close();
    }
  }

  @Test
  void testPost_UnknownMessageType() throws Exception {
    when(ctx.accept(TEXT_EVENT_STREAM)).thenReturn(true);
    when(ctx.accept(MediaType.json)).thenReturn(true);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.valueOrNull()).thenReturn("body");

    Value sessVal = mock(Value.class);
    when(sessVal.isMissing()).thenReturn(false);
    when(sessVal.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(sessVal);
    injectSession("sess-1", session);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      // Returning null simulates an unknown/unrecognized type gracefully bypassing the instanceof
      // checks
      schema.when(() -> McpSchema.deserializeJsonRpcMessage(jsonMapper, "body")).thenReturn(null);
      postHandler.apply(ctx);
    }
  }

  // --- DELETE ROUTE TESTS ---

  @Test
  void testDelete_IsClosing() throws Exception {
    provider.closeGracefully().block();
    deleteHandler.apply(ctx);
  }

  @Test
  void testDelete_DisallowDelete() throws Exception {
    Field field = StreamableTransportProvider.class.getDeclaredField("disallowDelete");
    field.setAccessible(true);
    field.set(provider, true);

    deleteHandler.apply(ctx);
  }

  @Test
  void testDelete_MissingSessionId() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(true);
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);
    deleteHandler.apply(ctx);
  }

  @Test
  void testDelete_SessionNotFound() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("unknown");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);
    deleteHandler.apply(ctx);
  }

  @Test
  void testDelete_Success() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);

    injectSession("sess-1", session);
    when(session.delete()).thenReturn(Mono.empty());

    Object res = deleteHandler.apply(ctx);
    assertEquals(StatusCode.NO_CONTENT, res);
  }

  @Test
  void testDelete_Exception() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.header(HttpHeaders.MCP_SESSION_ID)).thenReturn(val);

    injectSession("sess-1", session);
    when(session.delete()).thenThrow(new RuntimeException("Delete fail"));

    deleteHandler.apply(ctx);
  }

  // --- MISC / NOTIFY ---

  @Test
  void testNotifyClients_Empty() {
    provider.notifyClients("method", "params").block();
    verify(session, never()).sendNotification(anyString(), any());
  }

  @Test
  void testNotifyClients_Populated() throws Exception {
    injectSession("sess-1", session);
    when(session.sendNotification("method", "params")).thenReturn(Mono.empty());
    provider.notifyClients("method", "params").block();
    verify(session).sendNotification("method", "params");
  }
}
