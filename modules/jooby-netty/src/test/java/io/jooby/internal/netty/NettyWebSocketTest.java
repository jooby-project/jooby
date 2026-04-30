/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;
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

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.WebSocket;
import io.jooby.WebSocketCloseStatus;
import io.jooby.WebSocketMessage;
import io.jooby.output.Output;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;

@ExtendWith(MockitoExtension.class)
class NettyWebSocketTest {

  @Mock NettyContext netty;
  @Mock ChannelHandlerContext ctx;
  @Mock Channel channel;
  @Mock ChannelFuture closeFuture;
  @Mock ChannelFuture writeFuture;
  @Mock Attribute<NettyWebSocket> wsAttr;
  @Mock Route route;
  @Mock Router router;
  @Mock Logger logger;
  @Mock Executor worker;

  @BeforeEach
  void setup() {
    netty.ctx = ctx;
    lenient().when(ctx.channel()).thenReturn(channel);
    lenient().when(channel.attr(NettyWebSocket.WS)).thenReturn(wsAttr);
    lenient().when(channel.closeFuture()).thenReturn(closeFuture);
    lenient().when(netty.getRoute()).thenReturn(route);
    lenient().when(route.getPattern()).thenReturn("/ws");
    lenient().when(netty.getRouter()).thenReturn(router);
    lenient().when(router.getLog()).thenReturn(logger);
    lenient().when(router.getWorker()).thenReturn(worker);
  }

  @AfterEach
  void tearDown() {
    NettyWebSocket.all.clear();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testConstructorAndGetters() {
    when(netty.isInIoThread()).thenReturn(true); // dispatch = false
    NettyWebSocket ws = new NettyWebSocket(netty);

    verify(wsAttr).set(ws);

    // FIX: Use GenericFutureListener to properly capture the Netty lambda
    ArgumentCaptor<io.netty.util.concurrent.GenericFutureListener> captor =
        ArgumentCaptor.forClass(io.netty.util.concurrent.GenericFutureListener.class);
    verify(closeFuture).addListener(captor.capture());

    // Test closeFuture listener triggers handleClose
    when(channel.isOpen()).thenReturn(true);
    ws.fireConnect(); // make open

    try {
      captor.getValue().operationComplete(closeFuture);
    } catch (Exception e) {
      // Ignore generic signature throws for the test
    }
    assertFalse(ws.isOpen());

    assertNotNull(ws.getContext());
  }

  @Test
  void testLifecycleCallbacksAndDispatch() {
    // Force dispatch = true
    when(netty.isInIoThread()).thenReturn(false);
    NettyWebSocket ws = new NettyWebSocket(netty);

    WebSocket.OnConnect onConnect = mock(WebSocket.OnConnect.class);
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);
    WebSocket.OnError onError = mock(WebSocket.OnError.class);

    ws.onConnect(onConnect).onMessage(onMessage).onClose(onClose).onError(onError);

    ws.fireConnect();

    // Verify it was sent to worker since dispatch = true
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(worker, atLeastOnce()).execute(runnableCaptor.capture());

    // Run the captured connect task
    runnableCaptor.getAllValues().get(0).run();
    verify(onConnect).onConnect(ws);

    // Verify getSessions includes other sessions but not self
    NettyWebSocket otherWs = new NettyWebSocket(netty);
    otherWs.fireConnect();
    List<WebSocket> sessions = ws.getSessions();
    assertEquals(1, sessions.size());
    assertTrue(sessions.contains(otherWs));
    assertFalse(sessions.contains(ws));
  }

  @Test
  void testSendMethodsAndWriteCallbackAdaptor() throws Exception {
    when(netty.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    when(channel.writeAndFlush(any())).thenReturn(writeFuture);

    NettyWebSocket ws = new NettyWebSocket(netty);
    ws.fireConnect();

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    // Test send String
    ws.send("test", callback);
    verify(channel).writeAndFlush(any(TextWebSocketFrame.class));

    // Test send byte[]
    ws.send(new byte[] {1, 2}, callback);

    // Test send ByteBuffer
    ws.send(ByteBuffer.wrap(new byte[] {1}), callback);

    // Test sendBinary ByteBuffer
    ws.sendBinary(ByteBuffer.wrap(new byte[] {1}), callback);

    // Test sendBinary String
    ws.sendBinary("binary", callback);

    // Test sendBinary byte[]
    ws.sendBinary(new byte[] {1, 2}, callback);

    // Test sendPing String
    ws.sendPing("ping", callback);
    verify(channel).writeAndFlush(any(PingWebSocketFrame.class));

    // Test sendPing ByteBuffer
    ws.sendPing(ByteBuffer.wrap(new byte[] {1}), callback);

    // Capture WriteCallbackAdaptor and test success/error handling
    ArgumentCaptor<ChannelFutureListener> captor =
        ArgumentCaptor.forClass(ChannelFutureListener.class);
    verify(writeFuture, atLeastOnce()).addListener(captor.capture());
    ChannelFutureListener adaptor = captor.getValue();

    // 1. Success
    when(writeFuture.cause()).thenReturn(null);
    adaptor.operationComplete(writeFuture);
    verify(callback).operationComplete(ws, null);

    // 2. Server.connectionLost = true
    RuntimeException cause1 = new RuntimeException("lost");
    when(writeFuture.cause()).thenReturn(cause1);
    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      serverMock.when(() -> Server.connectionLost(cause1)).thenReturn(true);
      adaptor.operationComplete(writeFuture);
      verify(logger).debug(anyString(), any(), eq(cause1));
      verify(callback).operationComplete(ws, cause1);
    }

    // 3. Server.connectionLost = false
    Exception cause2 = new Exception("error");
    when(writeFuture.cause()).thenReturn(cause2);
    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      serverMock.when(() -> Server.connectionLost(cause2)).thenReturn(false);
      adaptor.operationComplete(writeFuture);
      verify(logger).error(anyString(), any(), eq(cause2));
      verify(callback).operationComplete(ws, cause2);
    }
  }

  @Test
  void testSendWhenClosed() {
    when(netty.isInIoThread()).thenReturn(true);

    NettyWebSocket ws = new NettyWebSocket(netty);
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    ws.send("test", callback);
    // Should trigger handleError, which logs since no onError is set
    verify(logger).error(anyString(), any(), any(IllegalStateException.class));
  }

  @Test
  void testSendOutput() {
    when(netty.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    when(channel.writeAndFlush(any())).thenReturn(writeFuture);

    NettyWebSocket ws = new NettyWebSocket(netty);
    ws.fireConnect();

    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);
    Output output = mock(Output.class);

    try (MockedStatic<NettyByteBufRef> bufRefMock = mockStatic(NettyByteBufRef.class)) {
      bufRefMock
          .when(() -> NettyByteBufRef.byteBuf(output))
          .thenReturn(Unpooled.wrappedBuffer(new byte[] {1}));

      ws.send(output, callback);
      verify(channel).writeAndFlush(any(TextWebSocketFrame.class));

      ws.sendBinary(output, callback);
      verify(channel).writeAndFlush(any(BinaryWebSocketFrame.class));
    }
  }

  @Test
  void testRender() {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws = new NettyWebSocket(netty);
    WebSocket.WriteCallback callback = mock(WebSocket.WriteCallback.class);

    try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
      Context wsc = mock(Context.class);
      contextMock.when(() -> Context.websocket(any(), any(), anyBoolean(), any())).thenReturn(wsc);

      ws.render("value", callback);
      verify(wsc).render("value");

      ws.renderBinary("value", callback);
      verify(wsc, times(2)).render("value");

      // Test render error
      contextMock
          .when(() -> Context.websocket(any(), any(), anyBoolean(), any()))
          .thenThrow(new RuntimeException("render fail"));
      ws.render("value", callback);
      verify(logger).error(anyString(), any(), any(RuntimeException.class));
    }
  }

  @Test
  void testHandleFrame_Ping() {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws = new NettyWebSocket(netty);
    ws.fireConnect();

    when(channel.writeAndFlush(any())).thenReturn(writeFuture);

    PingWebSocketFrame ping = mock(PingWebSocketFrame.class);
    when(ping.content()).thenReturn(Unpooled.wrappedBuffer(new byte[] {1}));

    ws.handleFrame(ping);

    verify(channel).writeAndFlush(any(PongWebSocketFrame.class));
    verify(ping).release();
  }

  @Test
  void testHandleFrame_TextAndFragmented() {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws = new NettyWebSocket(netty);
    WebSocket.OnMessage onMessage = mock(WebSocket.OnMessage.class);
    ws.onMessage(onMessage);
    ws.fireConnect();

    // 1. Initial non-final fragment
    TextWebSocketFrame f1 = mock(TextWebSocketFrame.class);
    when(f1.isFinalFragment()).thenReturn(false);
    when(f1.content()).thenReturn(Unpooled.wrappedBuffer(new byte[] {1, 2}));

    ws.handleFrame(f1);
    verify(f1).release();

    // 2. Final continuation fragment
    ContinuationWebSocketFrame f2 = mock(ContinuationWebSocketFrame.class);
    when(f2.isFinalFragment()).thenReturn(true);
    when(f2.content()).thenReturn(Unpooled.wrappedBuffer(new byte[] {3, 4}));

    ws.handleFrame(f2);
    verify(f2).release();

    // onMessage should have been called with combined buffer [1, 2, 3, 4]
    verify(onMessage).onMessage(eq(ws), any(WebSocketMessage.class));
  }

  @Test
  void testHandleFrame_Close() {
    when(netty.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    when(channel.writeAndFlush(any())).thenReturn(writeFuture);
    NettyWebSocket ws = new NettyWebSocket(netty);
    WebSocket.OnClose onClose = mock(WebSocket.OnClose.class);
    ws.onClose(onClose);
    ws.fireConnect();

    CloseWebSocketFrame closeFrame = mock(CloseWebSocketFrame.class);
    when(closeFrame.statusCode()).thenReturn(1000);

    ws.handleFrame(closeFrame);

    verify(channel).writeAndFlush(any(CloseWebSocketFrame.class));
    verify(closeFrame).release();
    verify(wsAttr).set(null); // Session removed
    verify(onClose).onClose(eq(ws), any(WebSocketCloseStatus.class));
  }

  @Test
  void testHandleError_Fatal() {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws = new NettyWebSocket(netty);
    ws.fireConnect();

    try (MockedStatic<SneakyThrows> st = mockStatic(SneakyThrows.class)) {
      st.when(() -> SneakyThrows.isFatal(any())).thenReturn(true);
      st.when(() -> SneakyThrows.propagate(any())).thenReturn(new RuntimeException("Fatal Error"));

      TextWebSocketFrame badFrame = mock(TextWebSocketFrame.class);
      ws.onMessage(mock(WebSocket.OnMessage.class)); // Register callback to ensure frame is read
      when(badFrame.isFinalFragment()).thenThrow(new RuntimeException("Boom"));

      assertThrows(RuntimeException.class, () -> ws.handleFrame(badFrame));

      verify(wsAttr, atLeastOnce()).set(null); // Cleanup called
    }
  }

  @Test
  void testHandleError_ConnectionLostWithCallback() {
    when(netty.isInIoThread()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    when(channel.writeAndFlush(any())).thenReturn(writeFuture);

    NettyWebSocket ws = new NettyWebSocket(netty);
    ws.fireConnect();
    WebSocket.OnError onError = mock(WebSocket.OnError.class);
    ws.onError(onError);

    RuntimeException connectionLostEx = new RuntimeException("Lost");

    try (MockedStatic<Server> sm = mockStatic(Server.class)) {
      sm.when(() -> Server.connectionLost(connectionLostEx)).thenReturn(true);

      // Simulate exception via render
      try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
        contextMock
            .when(() -> Context.websocket(any(), any(), anyBoolean(), any()))
            .thenThrow(connectionLostEx);
        ws.render("fail", mock(WebSocket.WriteCallback.class));
      }

      verify(channel)
          .writeAndFlush(any(CloseWebSocketFrame.class)); // Closed due to connection loss
      verify(onError).onError(ws, connectionLostEx); // Custom error handler invoked
    }
  }

  @Test
  void testForEach() {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws1 = new NettyWebSocket(netty);
    ws1.fireConnect();

    // Simulate exception in consumer
    ws1.forEach(
        ws -> {
          throw new RuntimeException("Consumer fail");
        });

    verify(logger).debug(anyString(), any(), any(RuntimeException.class));
  }

  @Test
  void testWaitForConnectInterrupted() throws InterruptedException {
    when(netty.isInIoThread()).thenReturn(true);
    NettyWebSocket ws = new NettyWebSocket(netty);

    AtomicBoolean wasInterrupted = new AtomicBoolean(false);

    Thread t =
        new Thread(
            () -> {
              Thread.currentThread().interrupt(); // Interrupt early
              ws.handleFrame(mock(TextWebSocketFrame.class)); // Triggers waitForConnect()
              wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

    t.start();
    t.join();

    assertTrue(wasInterrupted.get(), "Thread interrupt flag should be restored");
  }

  @Test
  void testEmptySessions() {
    when(netty.isInIoThread()).thenReturn(true);
    // Use an unmapped key
    when(route.getPattern()).thenReturn("/empty");
    NettyWebSocket ws = new NettyWebSocket(netty);

    assertTrue(ws.getSessions().isEmpty());
  }
}
