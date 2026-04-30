/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.xnio.IoUtils;
import org.xnio.Pooled;

import com.typesafe.config.Config;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.output.Output;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

@ExtendWith(MockitoExtension.class)
class UndertowWebSocketTest {

  @Mock UndertowContext ctx;
  @Mock WebSocketChannel channel;
  @Mock Router router;
  @Mock Config config;
  @Mock Route route;
  @Mock Logger logger;
  @Mock Executor worker;

  @BeforeEach
  void setup() {
    lenient().when(ctx.getRouter()).thenReturn(router);
    lenient().when(router.getConfig()).thenReturn(config);
    lenient().when(router.getLog()).thenReturn(logger);
    lenient().when(router.getWorker()).thenReturn(worker);
    lenient().when(ctx.getRoute()).thenReturn(route);
    lenient().when(route.getPattern()).thenReturn("/ws");

    // Provide a lenient default for all config path checks
    lenient().when(config.hasPath(anyString())).thenReturn(false);

    // Mock the receive setter to prevent the hidden NullPointerException during fireConnect()
    lenient()
        .when(channel.getReceiveSetter())
        .thenReturn(mock(org.xnio.ChannelListener.Setter.class));
  }

  @AfterEach
  void tearDown() {
    UndertowWebSocket.all.clear();
  }

  @Test
  void testConstructorAndBufferSizes() {
    when(config.hasPath("websocket.maxSize")).thenReturn(true);
    when(config.getBytes("websocket.maxSize")).thenReturn(1024L);

    when(ctx.isInIoThread()).thenReturn(true);

    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    assertEquals(1024L, ws.getMaxTextBufferSize());
    assertEquals(1024L, ws.getMaxBinaryBufferSize());
    assertNotNull(ws.getContext());
  }

  @Test
  void testConstructorDefaultBufferSizes() {
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    assertEquals(WebSocket.MAX_BUFFER_SIZE, ws.getMaxTextBufferSize());
    assertEquals(WebSocket.MAX_BUFFER_SIZE, ws.getMaxBinaryBufferSize());
  }

  @Test
  void testFireConnectAndDispatch() {
    when(ctx.isInIoThread()).thenReturn(false); // dispatch = true
    lenient().when(config.hasPath("websocket.idleTimeout")).thenReturn(true);
    lenient()
        .when(config.getDuration("websocket.idleTimeout", TimeUnit.MILLISECONDS))
        .thenReturn(5000L);

    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    WebSocket.OnConnect onConnect = mock(WebSocket.OnConnect.class);
    ws.onConnect(onConnect);

    ws.fireConnect();

    verify(channel).setIdleTimeout(5000L);
    verify(channel).resumeReceives();

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(worker).execute(captor.capture());

    captor.getValue().run();
    verify(onConnect).onConnect(ws);
  }

  @Test
  void testFireConnect_DefaultTimeout_NoConnectCallback() {
    when(ctx.isInIoThread()).thenReturn(true);

    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    verify(channel).setIdleTimeout(TimeUnit.MINUTES.toMillis(5));
    verify(channel).resumeReceives();

    UndertowWebSocket ws2 = new UndertowWebSocket(ctx, channel);
    ws2.fireConnect();

    assertEquals(1, ws.getSessions().size());
    assertTrue(ws.getSessions().contains(ws2));
  }

  @Test
  void testFireConnectThrowsException() {
    when(ctx.isInIoThread()).thenReturn(true);
    doThrow(new RuntimeException("Connect Error")).when(channel).setIdleTimeout(anyLong());

    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    verify(logger).error(anyString(), any(), any(RuntimeException.class));
  }

  @Test
  void testBroadcastForEach() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    ws.forEach(
        webSocket -> {
          assertEquals(ws, webSocket);
        });

    ws.forEach(
        webSocket -> {
          throw new RuntimeException("Broadcast fail");
        });

    verify(logger).debug(anyString(), any(), any(RuntimeException.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSendMethods() {
    when(ctx.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);

    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      ws.sendPing("ping", callback);
      ws.sendPing(ByteBuffer.wrap(new byte[] {1}), callback);
      webSockets.verify(
          () -> WebSockets.sendPing(any(ByteBuffer.class), eq(channel), any()), times(2));

      ws.send("text", callback);
      ws.send(ByteBuffer.wrap(new byte[] {1}), callback);
      webSockets.verify(
          () -> WebSockets.sendText(any(ByteBuffer.class), eq(channel), any()), times(2));

      ws.sendBinary("binary", callback);
      ws.sendBinary(ByteBuffer.wrap(new byte[] {1}), callback);
      webSockets.verify(
          () -> WebSockets.sendBinary(any(ByteBuffer.class), eq(channel), any()), times(2));

      UndertowWebSocket.sendMessage(
          ws,
          ByteBuffer.wrap(new byte[] {1}),
          UndertowWebSocket.FrameType.PONG,
          mock(WebSocketCallback.class));
      webSockets.verify(() -> WebSockets.sendPong(any(ByteBuffer.class), eq(channel), any()));

      // Instead of assertThrows, verify it was caught and routed to logger
      UndertowWebSocket.sendMessage(
          ws, ByteBuffer.wrap(new byte[] {1}), null, mock(WebSocketCallback.class));
      verify(logger).error(anyString(), any(), any(IllegalStateException.class));
    }
  }

  @Test
  void testSendThrowsException() {
    when(ctx.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      webSockets
          .when(() -> WebSockets.sendText(any(ByteBuffer.class), any(), any()))
          .thenThrow(new RuntimeException("Send failed"));

      ws.send("test", callback);
      verify(logger).error(anyString(), any(), any(RuntimeException.class));
    }
  }

  @Test
  void testSendWhenClosed() {
    // Because open.get() is false, short-circuiting ensures channel.isOpen() is never called.
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    ws.send("test", callback);

    verify(logger).error(anyString(), any(), any(IllegalStateException.class));
  }

  @Test
  void testSendOutput() {
    when(ctx.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    Output output = mock(Output.class);
    Iterator<ByteBuffer> iterator =
        Arrays.asList(ByteBuffer.wrap(new byte[] {1}), ByteBuffer.wrap(new byte[] {2})).iterator();
    when(output.iterator()).thenReturn(iterator);

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      ws.send(output, callback);
      ws.sendBinary(output, callback);

      ArgumentCaptor<WebSocketCallback> callbackCaptor =
          ArgumentCaptor.forClass(WebSocketCallback.class);
      webSockets.verify(
          () -> WebSockets.sendText(any(ByteBuffer.class), eq(channel), callbackCaptor.capture()));

      callbackCaptor.getValue().complete(channel, null);

      callbackCaptor.getValue().onError(channel, null, new RuntimeException("Chunk fail"));
      verify(logger).error(anyString(), any(), any(RuntimeException.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testWriteCallbackAdaptor() {
    when(ctx.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    ws.fireConnect();

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      ws.send("test", callback);

      ArgumentCaptor<WebSocketCallback> captor = ArgumentCaptor.forClass(WebSocketCallback.class);
      webSockets.verify(
          () -> WebSockets.sendText(any(ByteBuffer.class), eq(channel), captor.capture()));
      WebSocketCallback<Void> adaptor = captor.getValue();

      adaptor.complete(channel, null);
      verify(callback).operationComplete(ws, null);

      Exception lostEx = new Exception("Lost");
      try (MockedStatic<Server> server = mockStatic(Server.class)) {
        server.when(() -> Server.connectionLost(lostEx)).thenReturn(true);
        adaptor.onError(channel, null, lostEx);
        verify(logger).debug(anyString(), any(), eq(lostEx));
        verify(callback).operationComplete(ws, lostEx);
      }

      Exception genEx = new Exception("General");
      try (MockedStatic<Server> server = mockStatic(Server.class)) {
        server.when(() -> Server.connectionLost(genEx)).thenReturn(false);
        adaptor.onError(channel, null, genEx);
        verify(logger).error(anyString(), any(), eq(genEx));
        verify(callback).operationComplete(ws, genEx);
      }
    }
  }

  @Test
  void testRender() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      Context wsc = mock(Context.class);
      contextMock.when(() -> Context.websocket(any(), any(), anyBoolean(), any())).thenReturn(wsc);

      ws.render("value", callback);
      verify(wsc).render("value");

      ws.renderBinary("value", callback);
      verify(wsc, times(2)).render("value");

      contextMock
          .when(() -> Context.websocket(any(), any(), anyBoolean(), any()))
          .thenThrow(new RuntimeException("Render Error"));
      ws.render("value", callback);
      verify(logger).error(anyString(), any(), any(RuntimeException.class));
    }
  }

  @Test
  void testOnFullTextMessage() throws IOException {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);

    ws.fireConnect();

    BufferedTextMessage textMsg = mock(BufferedTextMessage.class);
    when(textMsg.getData()).thenReturn("Hello");

    ws.onFullTextMessage(channel, textMsg);

    verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
  }

  @Test
  void testOnFullBinaryMessage_HeapBuffer() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);
    ws.fireConnect();

    BufferedBinaryMessage binMsg = mock(BufferedBinaryMessage.class);
    Pooled<ByteBuffer[]> pooled = mock(Pooled.class);
    when(binMsg.getData()).thenReturn(pooled);
    when(pooled.getResource()).thenReturn(new ByteBuffer[] {});

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      webSockets
          .when(() -> WebSockets.mergeBuffers(any(ByteBuffer[].class)))
          .thenReturn(ByteBuffer.allocate(10));

      ws.onFullBinaryMessage(channel, binMsg);

      verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
      verify(pooled).free();
    }
  }

  @Test
  void testOnFullBinaryMessage_DirectBuffer() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);
    ws.fireConnect();

    BufferedBinaryMessage binMsg = mock(BufferedBinaryMessage.class);
    Pooled<ByteBuffer[]> pooled = mock(Pooled.class);
    when(binMsg.getData()).thenReturn(pooled);
    when(pooled.getResource()).thenReturn(new ByteBuffer[] {});

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class)) {
      webSockets
          .when(() -> WebSockets.mergeBuffers(any(ByteBuffer[].class)))
          .thenReturn(ByteBuffer.allocateDirect(10));

      ws.onFullBinaryMessage(channel, binMsg);

      verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
      verify(pooled).free();
    }
  }

  @Test
  void testWaitForConnectInterrupted() throws InterruptedException {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    AtomicBoolean wasInterrupted = new AtomicBoolean(false);
    Thread t =
        new Thread(
            () -> {
              Thread.currentThread().interrupt();
              try {
                ws.onFullTextMessage(channel, mock(BufferedTextMessage.class));
              } catch (IOException e) {
                // ignore
              }
              wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

    t.start();
    t.join();

    assertTrue(wasInterrupted.get(), "Thread interrupt flag should be restored");
  }

  @Test
  void testOnError_Fatal() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    try (MockedStatic<SneakyThrows> st = mockStatic(SneakyThrows.class)) {
      st.when(() -> SneakyThrows.isFatal(any())).thenReturn(true);
      st.when(() -> SneakyThrows.propagate(any())).thenReturn(new RuntimeException("Fatal Error"));

      Runnable task =
          () -> {
            throw new RuntimeException("Boom");
          };

      assertThrows(
          RuntimeException.class,
          () -> {
            try {
              java.lang.reflect.Method m =
                  UndertowWebSocket.class.getDeclaredMethod(
                      "webSocketTask", Runnable.class, boolean.class);
              m.setAccessible(true);
              ((Runnable) m.invoke(ws, task, false)).run();
            } catch (Exception e) {
              throw new RuntimeException(e.getCause());
            }
          });
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHandleClose_And_OnCloseMessage() {
    when(ctx.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);
    ws.onClose(onClose);
    ws.fireConnect();

    try (MockedStatic<WebSockets> webSockets = mockStatic(WebSockets.class);
        MockedStatic<IoUtils> ioUtils = mockStatic(IoUtils.class)) {

      CloseMessage closeMessage = new CloseMessage(1000, "Normal");

      ws.onCloseMessage(closeMessage, channel);

      ArgumentCaptor<WebSocketCallback> callbackCaptor =
          ArgumentCaptor.forClass(WebSocketCallback.class);
      webSockets.verify(
          () ->
              WebSockets.sendClose(
                  eq(1000), eq("Normal"), eq(channel), callbackCaptor.capture(), eq(ws)));

      callbackCaptor.getValue().complete(channel, ws);
      ioUtils.verify(() -> IoUtils.safeClose(channel));

      callbackCaptor.getValue().onError(channel, ws, new RuntimeException("Close Error"));
      ioUtils.verify(() -> IoUtils.safeClose(channel), times(2));

      verify(onClose).onClose(eq(ws), any(WebSocketCloseStatus.class));
      assertFalse(ws.getSessions().contains(ws));
    }
  }

  @Test
  void testHandleClose_ThrowsException() {
    when(ctx.isInIoThread()).thenReturn(true);
    UndertowWebSocket ws = new UndertowWebSocket(ctx, channel);

    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);
    doThrow(new RuntimeException("Callback Crash")).when(onClose).onClose(any(), any());
    ws.onClose(onClose);

    ws.close(WebSocketCloseStatus.NORMAL);

    verify(logger).error(anyString(), any(), any(RuntimeException.class));
  }
}
