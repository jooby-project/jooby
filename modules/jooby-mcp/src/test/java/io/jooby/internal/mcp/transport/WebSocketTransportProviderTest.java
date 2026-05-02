/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketConfigurer;
import io.jooby.WebSocketMessage;
import io.jooby.internal.mcp.McpServerConfig;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class WebSocketTransportProviderTest {

  @Mock Jooby app;
  @Mock McpServerConfig serverConfig;
  @Mock McpJsonMapper mcpJsonMapper;
  @Mock McpTransportContextExtractor<Context> contextExtractor;
  @Mock McpTransportContext transportContext;
  @Mock McpServerSession.Factory sessionFactory;
  @Mock McpServerSession session;
  @Mock WebSocketConfigurer wsConfigurer;
  @Mock WebSocket ws;
  @Mock Context ctx;
  @Mock Logger mockLogger;

  private WebSocketTransportProvider provider;

  private WebSocket.OnConnect onConnect;
  private WebSocket.OnMessage onMessage;
  private WebSocket.OnClose onClose;
  private WebSocket.OnError onError;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(serverConfig.getMcpEndpoint()).thenReturn("/mcp/ws");

    // Stub extractor to prevent downstream Reactor Context NPEs
    lenient().when(contextExtractor.extract(any())).thenReturn(transportContext);

    ArgumentCaptor<WebSocket.Initializer> initCap =
        ArgumentCaptor.forClass(WebSocket.Initializer.class);

    provider = new WebSocketTransportProvider(app, serverConfig, mcpJsonMapper, contextExtractor);
    provider.setSessionFactory(sessionFactory);

    // Inject mock logger into the provider to verify void log-based branches
    Field logField = AbstractMcpTransportProvider.class.getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(provider, mockLogger);

    // Capture the route setup
    verify(app).ws(eq("/mcp/ws"), initCap.capture());
    WebSocket.Initializer initializer = initCap.getValue();

    // Trigger the setup lambda to register the callbacks on our mock wsConfigurer
    initializer.init(ctx, wsConfigurer);

    // Capture the individual callbacks
    ArgumentCaptor<WebSocket.OnConnect> connectCap =
        ArgumentCaptor.forClass(WebSocket.OnConnect.class);
    ArgumentCaptor<WebSocket.OnMessage> messageCap =
        ArgumentCaptor.forClass(WebSocket.OnMessage.class);
    ArgumentCaptor<WebSocket.OnClose> closeCap = ArgumentCaptor.forClass(WebSocket.OnClose.class);
    ArgumentCaptor<WebSocket.OnError> errorCap = ArgumentCaptor.forClass(WebSocket.OnError.class);

    verify(wsConfigurer).onConnect(connectCap.capture());
    verify(wsConfigurer).onMessage(messageCap.capture());
    verify(wsConfigurer).onClose(closeCap.capture());
    verify(wsConfigurer).onError(errorCap.capture());

    onConnect = connectCap.getValue();
    onMessage = messageCap.getValue();
    onClose = closeCap.getValue();
    onError = errorCap.getValue();
  }

  // --- CORE CONFIG ---

  @Test
  void testTransportName() {
    assertEquals("WebSocket", provider.transportName());
  }

  // --- ON CONNECT TESTS ---

  @Test
  void testHandleConnect_Success() {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");

    onConnect.onConnect(ws);

    verify(ws).attribute("mcpSessionId", "sess-1");
    verify(mockLogger).debug("New WebSocket connection established. Session ID: {}", "sess-1");
  }

  @Test
  void testHandleConnect_WhenClosing() {
    // Initiate shutdown which sets isClosing = true
    provider.closeGracefully().block();

    onConnect.onConnect(ws);

    verify(ws).close(WebSocketCloseStatus.SERVICE_RESTARTED);
    verify(sessionFactory, never()).create(any());
  }

  // --- ON MESSAGE TESTS ---

  @Test
  void testHandleMessage_MissingSessionIdAttribute() {
    when(ws.attribute("mcpSessionId")).thenReturn(null);

    onMessage.onMessage(ws, mock(WebSocketMessage.class));

    verify(mockLogger)
        .warn("Received message on unknown or orphaned WS session ID: {}", (Object) null);
  }

  @Test
  void testHandleMessage_OrphanedSessionId() {
    when(ws.attribute("mcpSessionId")).thenReturn("unknown-session");

    onMessage.onMessage(ws, mock(WebSocketMessage.class));

    verify(mockLogger)
        .warn("Received message on unknown or orphaned WS session ID: {}", "unknown-session");
  }

  @Test
  void testHandleMessage_Success() throws Exception {
    setupActiveSession("sess-1");

    WebSocketMessage msg = mock(WebSocketMessage.class);
    when(msg.value()).thenReturn("msg-payload");
    when(ws.getContext()).thenReturn(ctx);

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "msg-payload"))
          .thenReturn(notif);
      when(session.handle(notif)).thenReturn(Mono.empty());

      onMessage.onMessage(ws, msg);

      verify(session).handle(notif);
      verify(mockLogger, never())
          .error(anyString(), anyString(), anyString()); // Ensure no errors logged
    }
  }

  @Test
  void testHandleMessage_DownstreamStreamError() throws Exception {
    setupActiveSession("sess-1");

    WebSocketMessage msg = mock(WebSocketMessage.class);
    when(msg.value()).thenReturn("msg-payload");
    when(ws.getContext()).thenReturn(ctx);

    McpSchema.JSONRPCNotification notif = mock(McpSchema.JSONRPCNotification.class);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "msg-payload"))
          .thenReturn(notif);

      // Simulate an error occurring within the reactor stream
      when(session.handle(notif))
          .thenReturn(Mono.error(new RuntimeException("Stream Processing Failed")));

      onMessage.onMessage(ws, msg);

      // Verify the subscribe error callback logged the issue
      verify(mockLogger)
          .error("Error processing WS message for {}: {}", "sess-1", "Stream Processing Failed");
    }
  }

  @Test
  void testHandleMessage_DeserializationError() throws Exception {
    setupActiveSession("sess-1");

    WebSocketMessage msg = mock(WebSocketMessage.class);
    when(msg.value()).thenReturn("msg-payload");
    when(ws.getContext()).thenReturn(ctx);

    try (MockedStatic<McpSchema> schema = mockStatic(McpSchema.class)) {
      schema
          .when(() -> McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, "msg-payload"))
          .thenThrow(new IllegalArgumentException("Format Invalid"));

      onMessage.onMessage(ws, msg);

      verify(mockLogger).error("Failed to deserialize WS message: {}", "Format Invalid");
    }
  }

  // --- ON CLOSE TESTS ---

  @Test
  void testHandleClose_Success() throws Exception {
    setupActiveSession("sess-1");
    when(ws.attribute("mcpSessionId")).thenReturn("sess-1");

    onClose.onClose(ws, WebSocketCloseStatus.NORMAL);

    verify(mockLogger)
        .debug(
            "WebSocket connection closed for session: {} with status: {}",
            "sess-1",
            WebSocketCloseStatus.NORMAL.getCode());

    // Verify it was cleared from the map
    Field mapField = AbstractMcpTransportProvider.class.getDeclaredField("sessions");
    mapField.setAccessible(true);
    ConcurrentHashMap map = (ConcurrentHashMap) mapField.get(provider);
    assertEquals(0, map.size());
  }

  @Test
  void testHandleClose_NoSessionId() {
    when(ws.attribute("mcpSessionId")).thenReturn(null);

    // Should safely abort without throwing an NPE or logging closing details
    onClose.onClose(ws, WebSocketCloseStatus.NORMAL);

    verify(mockLogger, never()).debug(anyString(), anyString(), any());
  }

  // --- ON ERROR TESTS ---

  @Test
  void testHandleError() {
    when(ws.attribute("mcpSessionId")).thenReturn("sess-1");
    Throwable exception = new RuntimeException("Socket disconnect");

    onError.onError(ws, exception);

    verify(mockLogger).error("WebSocket error for session: {}", "sess-1", exception);
  }

  // --- INNER TRANSPORT TESTS ---

  @Test
  void testInnerTransport_SendMessage_Success() throws Exception {
    McpServerTransport transport = setupAndCaptureInnerTransport();

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    when(mcpJsonMapper.writeValueAsString(msg)).thenReturn("{\"json\":\"rpc\"}");

    transport.sendMessage(msg).block();

    verify(ws).send("{\"json\":\"rpc\"}");
  }

  @Test
  void testInnerTransport_SendMessage_SerializationException() throws Exception {
    McpServerTransport transport = setupAndCaptureInnerTransport();

    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    RuntimeException ex = new RuntimeException("Serialization failure");
    when(mcpJsonMapper.writeValueAsString(msg)).thenThrow(ex);

    transport.sendMessage(msg).block();

    // Verify it was caught and logged by the inner class logger
    verify(mockLogger).error("Failed to send WebSocket message", ex);
  }

  @Test
  void testInnerTransport_Close_SuccessAndIdempotency() throws Exception {
    McpServerTransport transport = setupAndCaptureInnerTransport();

    // First close
    transport.closeGracefully().block();
    verify(ws).close(WebSocketCloseStatus.NORMAL);

    // Second close should be a no-op due to `closed` flag
    transport.closeGracefully().block();
    verify(ws, times(1)).close(any()); // Still only 1 invocation total

    // SendMessage after close should be a no-op
    McpSchema.JSONRPCNotification msg = mock(McpSchema.JSONRPCNotification.class);
    transport.sendMessage(msg).block();
    verify(ws, never()).send(anyString());
  }

  // --- HELPERS ---

  private void setupActiveSession(String id) {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn(id);
    onConnect.onConnect(ws); // Triggers creation and map population
    when(ws.attribute("mcpSessionId")).thenReturn(id);
  }

  private McpServerTransport setupAndCaptureInnerTransport() throws Exception {
    when(sessionFactory.create(any(McpServerTransport.class))).thenReturn(session);
    when(session.getId()).thenReturn("sess-1");

    onConnect.onConnect(ws);

    ArgumentCaptor<McpServerTransport> transportCap =
        ArgumentCaptor.forClass(McpServerTransport.class);
    verify(sessionFactory).create(transportCap.capture());

    McpServerTransport transport = transportCap.getValue();

    // The inner transport class creates its own logger via the AbstractMcpTransport superclass.
    // We must intercept it here to successfully verify the exception logging branches.
    Class<?> clazz = transport.getClass();
    while (clazz != null && clazz != Object.class) {
      try {
        Field logField = clazz.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(transport, mockLogger);
        break;
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }

    return transport;
  }
}
