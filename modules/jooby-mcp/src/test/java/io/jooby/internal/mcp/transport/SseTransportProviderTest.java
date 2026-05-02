/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static io.jooby.internal.mcp.transport.TransportConstants.MESSAGE_EVENT_TYPE;
import static io.jooby.internal.mcp.transport.TransportConstants.SSE_ERROR_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

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
import io.jooby.ServerSentEmitter;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.value.Value;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class SseTransportProviderTest {

  @Mock Jooby app;
  @Mock McpServerConfig serverConfig;
  @Mock McpJsonMapper mcpJsonMapper;
  @Mock McpTransportContextExtractor<Context> contextExtractor;
  @Mock McpTransportContext transportContext;
  @Mock McpServerSession.Factory sessionFactory;
  @Mock McpServerSession session;
  @Mock ServerSentEmitter sse;
  @Mock Context ctx;

  private SseTransportProvider provider;
  private Route.Handler headHandler;
  private ServerSentEmitter.Handler sseHandler;
  private Route.Handler postHandler;

  @BeforeEach
  void setup() {
    lenient().when(serverConfig.getMessageEndpoint()).thenReturn("/mcp/message");
    lenient().when(serverConfig.getSseEndpoint()).thenReturn("/mcp/sse");
    lenient().when(contextExtractor.extract(any())).thenReturn(transportContext);

    Route headRoute = mock(Route.class);
    lenient().when(headRoute.produces(any())).thenReturn(headRoute);
    lenient().when(headRoute.produces(any(MediaType.class))).thenReturn(headRoute);
    lenient().when(app.head(anyString(), any())).thenReturn(headRoute);

    ArgumentCaptor<Route.Handler> headCap = ArgumentCaptor.forClass(Route.Handler.class);
    ArgumentCaptor<ServerSentEmitter.Handler> sseCap =
        ArgumentCaptor.forClass(ServerSentEmitter.Handler.class);
    ArgumentCaptor<Route.Handler> postCap = ArgumentCaptor.forClass(Route.Handler.class);

    provider = new SseTransportProvider(app, serverConfig, mcpJsonMapper, contextExtractor);
    provider.setSessionFactory(sessionFactory);

    verify(app).head(eq("/mcp/sse"), headCap.capture());
    verify(app).sse(eq("/mcp/sse"), sseCap.capture());
    verify(app).post(eq("/mcp/message"), postCap.capture());

    headHandler = headCap.getValue();
    sseHandler = sseCap.getValue();
    postHandler = postCap.getValue();
  }

  private void injectSession(String id, McpServerSession sess) throws Exception {
    Field field = AbstractMcpTransportProvider.class.getDeclaredField("sessions");
    field.setAccessible(true);
    ((ConcurrentHashMap<String, McpServerSession>) field.get(provider)).put(id, sess);
  }

  // --- CORE METHODS ---

  @Test
  void testTransportName() {
    assertEquals("SSE", provider.transportName());
  }

  @Test
  void testHeadHandler() throws Exception {
    Object result = headHandler.apply(ctx);
    assertEquals(StatusCode.OK, result);
  }

  // --- SSE CONNECTION TESTS ---

  @Test
  void testHandleSseConnection() throws Exception {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-123");

    sseHandler.handle(sse);

    // Verify session was added to internal map by capturing and checking onClose
    ArgumentCaptor<SneakyThrows.Runnable> onCloseCap =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(sse).onClose(onCloseCap.capture());

    // Verify initial endpoint event is sent
    ArgumentCaptor<ServerSentMessage> msgCap = ArgumentCaptor.forClass(ServerSentMessage.class);
    verify(sse).send(msgCap.capture());
    assertEquals("endpoint", msgCap.getValue().getEvent());
    assertEquals("/mcp/message?sessionId=sess-123", msgCap.getValue().getData());

    // Trigger onClose to verify it removes the session
    onCloseCap.getValue().run();

    // Verify session map is empty after close
    Field field = AbstractMcpTransportProvider.class.getDeclaredField("sessions");
    field.setAccessible(true);
    ConcurrentHashMap map = (ConcurrentHashMap) field.get(provider);
    assertFalse(map.containsKey("sess-123"));
  }

  // --- INNER TRANSPORT CLASS TESTS ---

  @Test
  void testInnerTransport_SendMessage_Success() throws Exception {
    // Intercept the transport created during SSE connection
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");
    sseHandler.handle(sse);

    ArgumentCaptor<McpServerTransport> transportCap =
        ArgumentCaptor.forClass(McpServerTransport.class);
    verify(sessionFactory).create(transportCap.capture());
    McpServerTransport transport = transportCap.getValue();

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    when(mcpJsonMapper.writeValueAsString(msg)).thenReturn("{\"json\":\"rpc\"}");

    transport.sendMessage(msg).block();

    ArgumentCaptor<ServerSentMessage> sseMsgCap = ArgumentCaptor.forClass(ServerSentMessage.class);
    verify(sse, times(2))
        .send(sseMsgCap.capture()); // captures the second call (first was endpoint)

    assertEquals(MESSAGE_EVENT_TYPE, sseMsgCap.getAllValues().get(1).getEvent());
    assertEquals("{\"json\":\"rpc\"}", sseMsgCap.getAllValues().get(1).getData());
  }

  @Test
  void testInnerTransport_SendMessage_Exception() throws Exception {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");
    sseHandler.handle(sse);

    ArgumentCaptor<McpServerTransport> transportCap =
        ArgumentCaptor.forClass(McpServerTransport.class);
    verify(sessionFactory).create(transportCap.capture());
    McpServerTransport transport = transportCap.getValue();

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    when(mcpJsonMapper.writeValueAsString(msg))
        .thenThrow(new RuntimeException("JSON Serialization Failed"));

    transport.sendMessage(msg).block();

    verify(sse).send(eq(SSE_ERROR_EVENT), eq("JSON Serialization Failed"));
  }

  @Test
  void testInnerTransport_Close() throws Exception {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");
    sseHandler.handle(sse);

    ArgumentCaptor<McpServerTransport> transportCap =
        ArgumentCaptor.forClass(McpServerTransport.class);
    verify(sessionFactory).create(transportCap.capture());
    McpServerTransport transport = transportCap.getValue();

    transport.closeGracefully().block();
    verify(sse).close();
  }

  // --- POST MESSAGE ROUTE TESTS ---

  @Test
  void testHandleMessage_IsClosing() throws Exception {
    provider.closeGracefully().block();

    Object response = postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
    assertNotNull(response); // Returns McpError
  }

  @Test
  void testHandleMessage_MissingSessionId() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(true);
    when(ctx.query("sessionId")).thenReturn(val);

    Object response = postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.BAD_REQUEST);
    assertNotNull(response); // Returns McpError
  }

  @Test
  void testHandleMessage_SessionNotFound() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("invalid-session");
    when(ctx.query("sessionId")).thenReturn(val);

    Object response = postHandler.apply(ctx);

    verify(ctx).setResponseCode(StatusCode.NOT_FOUND);
    assertNotNull(response); // Returns McpError
  }

  @Test
  void testHandleMessage_Success() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.query("sessionId")).thenReturn(val);

    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.value()).thenReturn("payload");

    injectSession("sess-1", session);
    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "payload"))
          .thenReturn(msg);
      when(session.handle(msg))
          .thenReturn(Mono.empty()); // This will trigger switchIfEmpty(Mono.just(StatusCode.OK))

      Object response = postHandler.apply(ctx);

      assertEquals(StatusCode.OK, response);
    }
  }

  @Test
  void testHandleMessage_ProcessingError_ReturnsOkViaOnErrorResume() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.query("sessionId")).thenReturn(val);

    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.value()).thenReturn("payload");

    injectSession("sess-1", session);
    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "payload"))
          .thenReturn(msg);

      // Simulating a failure during handling, which should trigger the onErrorResume fallback
      when(session.handle(msg)).thenReturn(Mono.error(new RuntimeException("Handler crashed")));

      Object response = postHandler.apply(ctx);

      assertEquals(StatusCode.OK, response);
    }
  }

  @Test
  void testHandleMessage_DeserializationThrowsIOException() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.query("sessionId")).thenReturn(val);

    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.value()).thenReturn("payload");

    injectSession("sess-1", session);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "payload"))
          .thenThrow(new IOException("Stream failed"));

      Object response = postHandler.apply(ctx);

      assertNotNull(response); // Returns Parse Error McpError
    }
  }

  @Test
  void testHandleMessage_DeserializationThrowsIllegalArgumentException() throws Exception {
    Value val = mock(Value.class);
    when(val.isMissing()).thenReturn(false);
    when(val.value()).thenReturn("sess-1");
    when(ctx.query("sessionId")).thenReturn(val);

    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.value()).thenReturn("payload");

    injectSession("sess-1", session);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "payload"))
          .thenThrow(new IllegalArgumentException("Invalid format"));

      Object response = postHandler.apply(ctx);

      assertNotNull(response); // Returns Parse Error McpError
    }
  }
}
