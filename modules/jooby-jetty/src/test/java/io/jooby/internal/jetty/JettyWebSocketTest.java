/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.exceptions.CloseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.output.Output;

@ExtendWith(MockitoExtension.class)
class JettyWebSocketTest {

  @Mock JettyContext ctx;
  @Mock Router router;
  @Mock Route route;
  @Mock Logger logger;
  @Mock Session session;

  private JettyWebSocket ws;

  @BeforeEach
  void setup() {
    lenient().when(ctx.getRequestPath()).thenReturn("/ws");
    lenient().when(ctx.getRoute()).thenReturn(route);
    lenient().when(route.getPattern()).thenReturn("/ws");
    lenient().when(ctx.getRouter()).thenReturn(router);
    lenient().when(router.getLog()).thenReturn(logger);
    lenient().when(session.isOpen()).thenReturn(true);

    ws = new JettyWebSocket(ctx);
  }

  @AfterEach
  @SuppressWarnings("rawtypes")
  void tearDown() throws Exception {
    Field allField = JettyWebSocket.class.getDeclaredField("all");
    allField.setAccessible(true);
    ((ConcurrentMap) allField.get(null)).clear();
  }

  @Test
  void testLifecycleOpenAndClose() {
    WebSocket.OnConnect onConnect = mock(WebSocket.OnConnect.class);
    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);

    ws.onConnect(onConnect);
    ws.onClose(onClose);

    // Open
    ws.onWebSocketOpen(session);
    assertTrue(ws.isOpen());
    verify(onConnect).onConnect(ws);
    assertEquals(
        0, ws.getSessions().size()); // Note: getSessions excludes self, but let's test it with two

    // Close via Jetty
    ws.onWebSocketClose(1000, "Normal", null);
    assertFalse(ws.isOpen());
    verify(session).close(eq(1000), eq("Normal"), any(Callback.class));
    verify(onClose).onClose(eq(ws), any(WebSocketCloseStatus.class));
  }

  @Test
  void testOpenException() {
    WebSocket.OnConnect onConnect = mock(WebSocket.OnConnect.class);
    doThrow(new RuntimeException("Crash")).when(onConnect).onConnect(ws);
    ws.onConnect(onConnect);

    ws.onWebSocketOpen(session);

    // Handled by onWebSocketError
    verify(logger).error(anyString(), eq("/ws"), any(RuntimeException.class));
  }

  @Test
  void testBinaryMessage() {
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);

    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {1, 2, 3});
    ws.onWebSocketBinary(buffer, null);

    verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
  }

  @Test
  void testBinaryMessageException() {
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    doThrow(new RuntimeException("Crash")).when(onMessage).onMessage(any(), any());
    ws.onMessage(onMessage);

    ws.onWebSocketBinary(ByteBuffer.allocate(0), null);

    verify(logger).error(anyString(), eq("/ws"), any(RuntimeException.class));
  }

  @Test
  void testTextMessage() {
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);

    ws.onWebSocketText("Hello");

    verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
  }

  @Test
  void testTextMessageException() {
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    doThrow(new RuntimeException("Crash")).when(onMessage).onMessage(any(), any());
    ws.onMessage(onMessage);

    ws.onWebSocketText("Hello");

    verify(logger).error(anyString(), eq("/ws"), any(RuntimeException.class));
  }

  @Test
  void testOnWebSocketErrorTimeout() {
    CloseException timeout = new CloseException(1000, "Timeout", new TimeoutException());
    ws.onWebSocketError(timeout);

    // Timeout exceptions should be silently ignored
    verify(logger, never()).error(anyString(), any(), any());
    verify(logger, never()).debug(anyString(), any(), any());
  }

  @Test
  void testOnWebSocketErrorConnectionLost() {
    try (MockedStatic<Server> server = mockStatic(Server.class)) {
      server.when(() -> Server.connectionLost(any())).thenReturn(true);

      ws.onWebSocketOpen(session); // open session
      ws.onWebSocketError(new RuntimeException("Dropped"));

      verify(logger).debug(anyString(), eq("/ws"), any(RuntimeException.class));
      verify(session).close(eq(1011), anyString(), any()); // Triggers handleClose(SERVER_ERROR)
    }
  }

  @Test
  void testOnWebSocketErrorFatal() {
    try (MockedStatic<SneakyThrows> sneaky = mockStatic(SneakyThrows.class);
        MockedStatic<Server> server = mockStatic(Server.class)) {

      server.when(() -> Server.connectionLost(any())).thenReturn(false);
      sneaky.when(() -> SneakyThrows.isFatal(any())).thenReturn(true);
      sneaky.when(() -> SneakyThrows.propagate(any())).thenReturn(new RuntimeException("Fatal"));

      ws.onWebSocketOpen(session);

      assertThrows(RuntimeException.class, () -> ws.onWebSocketError(new OutOfMemoryError()));
      verify(session).close(eq(1011), anyString(), any()); // Triggers handleClose(SERVER_ERROR)
    }
  }

  @Test
  void testOnErrorCallback() {
    WebSocket.OnError onError = mock(WebSocket.OnError.class);
    ws.onError(onError);

    RuntimeException ex = new RuntimeException("Custom Error");
    ws.onWebSocketError(ex);

    verify(onError).onError(ws, ex);
  }

  @Test
  void testGetContext() {
    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      Context readOnly = mock(Context.class);
      contextMock.when(() -> Context.readOnly(ctx)).thenReturn(readOnly);

      assertEquals(readOnly, ws.getContext());
    }
  }

  @Test
  void testGetSessionsAndForEach() {
    // Before open
    assertEquals(Collections.emptyList(), ws.getSessions());

    ws.onWebSocketOpen(session);
    JettyWebSocket ws2 = new JettyWebSocket(ctx);
    ws2.onWebSocketOpen(session);

    List<WebSocket> sessions = ws.getSessions();
    assertEquals(1, sessions.size());
    assertEquals(ws2, sessions.get(0));

    // Test forEach
    SneakyThrows.Consumer<WebSocket> consumer = mock(SneakyThrows.Consumer.class);
    ws.forEach(consumer);
    verify(consumer).accept(ws);
    verify(consumer).accept(ws2);
  }

  @Test
  void testForEachException() {
    ws.onWebSocketOpen(session);

    SneakyThrows.Consumer<WebSocket> consumer = mock(SneakyThrows.Consumer.class);
    doThrow(new RuntimeException("Broadcast Fail")).when(consumer).accept(any());

    ws.forEach(consumer);
    verify(logger).debug(anyString(), eq("/ws"), any(RuntimeException.class));
  }

  @Test
  void testSendWhenClosed() {
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);
    ws.send("Test", cb);

    // Not open, triggers IllegalStateException -> onWebSocketError
    verify(logger).error(anyString(), eq("/ws"), any(IllegalStateException.class));
  }

  @Test
  void testSendMethods() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);

    ws.sendPing("ping", cb);
    verify(session).sendPing(any(ByteBuffer.class), any(Callback.class));

    ws.sendPing(ByteBuffer.wrap(new byte[] {1}), cb);
    verify(session, times(2)).sendPing(any(ByteBuffer.class), any(Callback.class));

    ws.send("text", cb);
    verify(session).sendText(eq("text"), any(Callback.class));

    ws.send(new byte[] {65}, cb);
    verify(session).sendText(eq("A"), any(Callback.class));

    ws.send(ByteBuffer.wrap(new byte[] {66}), cb);
    verify(session).sendText(eq("B"), any(Callback.class));

    ws.sendBinary("binary", cb);
    verify(session).sendBinary(any(ByteBuffer.class), any(Callback.class));

    ws.sendBinary(ByteBuffer.wrap(new byte[] {1}), cb);
    verify(session, times(2)).sendBinary(any(ByteBuffer.class), any(Callback.class));
  }

  @Test
  void testSendOutputMethods() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);
    Output output = mock(Output.class);
    when(output.asByteBuffer()).thenReturn(ByteBuffer.wrap(new byte[] {67}));

    ws.send(output, cb);
    verify(session).sendText(eq("C"), any(Callback.class));

    try (MockedConstruction<WebSocketOutputCallback> mocked =
        mockConstruction(WebSocketOutputCallback.class)) {
      ws.sendBinary(output, cb);
      verify(mocked.constructed().get(0)).send();
    }
  }

  @Test
  void testWriteCallbackAdaptor() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);

    ws.send("test", cb);

    ArgumentCaptor<Callback> captor = ArgumentCaptor.forClass(Callback.class);
    verify(session).sendText(eq("test"), captor.capture());
    Callback jettyCb = captor.getValue();

    // Test succeed
    jettyCb.succeed();
    verify(cb).operationComplete(ws, null);

    // Test fail
    Throwable error = new RuntimeException("Send Error");
    jettyCb.fail(error);
    verify(logger).error(anyString(), eq("/ws"), eq(error));
    verify(cb).operationComplete(ws, error);
  }

  @Test
  void testWriteCallbackAdaptorConnectionLost() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);

    ws.send("test", cb);

    ArgumentCaptor<Callback> captor = ArgumentCaptor.forClass(Callback.class);
    verify(session).sendText(eq("test"), captor.capture());
    Callback jettyCb = captor.getValue();

    try (MockedStatic<Server> server = mockStatic(Server.class)) {
      server.when(() -> Server.connectionLost(any())).thenReturn(true);
      Throwable error = new RuntimeException("Lost");

      jettyCb.fail(error);

      verify(logger).debug(anyString(), eq("/ws"), eq(error));
      verify(cb).operationComplete(ws, error);
    }
  }

  @Test
  void testRenderMethods() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);
    Object value = new Object();

    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      Context renderCtx = mock(Context.class);
      contextMock
          .when(() -> Context.websocket(eq(ctx), eq(ws), anyBoolean(), eq(cb)))
          .thenReturn(renderCtx);

      ws.render(value, cb);
      ws.renderBinary(value, cb);

      verify(renderCtx, times(2)).render(value);
    }
  }

  @Test
  void testRenderThrowsException() {
    ws.onWebSocketOpen(session);
    WebSocket.WriteCallback cb = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      contextMock
          .when(() -> Context.websocket(any(), any(), anyBoolean(), any()))
          .thenThrow(new RuntimeException("Render Failed"));

      ws.render("value", cb);

      verify(logger).error(anyString(), eq("/ws"), any(RuntimeException.class));
    }
  }

  @Test
  void testCloseWithSuppressedExceptions() {
    ws.onWebSocketOpen(session);

    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);
    doThrow(new RuntimeException("OnClose Exception")).when(onClose).onClose(any(), any());
    ws.onClose(onClose);

    doThrow(new RuntimeException("Session Close Exception"))
        .when(session)
        .close(anyInt(), anyString(), any());

    ws.close(WebSocketCloseStatus.NORMAL);

    // Verify outer catch caught it and logged it
    verify(logger).error(anyString(), eq("/ws"), any(RuntimeException.class));
  }
}
