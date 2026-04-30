/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AsciiString;
import io.netty.util.Attribute;
import io.netty.util.concurrent.EventExecutor;

@ExtendWith(MockitoExtension.class)
class NettyHandlerTest {

  @Mock NettyDateService serverDate;
  @Mock Context.Selector contextSelector;
  @Mock ChannelHandlerContext ctx;
  @Mock EventExecutor executor;
  @Mock Jooby app;
  @Mock Router router;
  @Mock Router.Match match;
  @Mock Channel channel;
  @Mock ChannelPromise responsePromise;
  @Mock ErrorHandler errorHandler;
  @Mock Logger appLogger;

  NettyHandler handler;

  @BeforeEach
  void setup() throws Exception {
    handler = new NettyHandler(serverDate, contextSelector, 1024, 10, 8192, true, false);

    lenient().when(ctx.executor()).thenReturn(executor);
    lenient().when(ctx.channel()).thenReturn(channel);
    lenient().when(serverDate.date()).thenReturn(new AsciiString("Wed, 21 Oct 2015 07:28:00 GMT"));

    lenient().when(contextSelector.select(anyString())).thenReturn(app);
    lenient().when(app.getRouter()).thenReturn(router);
    lenient().when(app.getTmpdir()).thenReturn(Paths.get("tmp"));
    lenient().when(router.match(any(Context.class))).thenReturn(match);

    lenient().when(app.getLog()).thenReturn(appLogger);
    lenient().when(router.getLog()).thenReturn(appLogger);

    lenient().when(app.getErrorHandler()).thenReturn(errorHandler);
    lenient().when(app.errorCode(any(Throwable.class))).thenReturn(StatusCode.SERVER_ERROR);

    lenient().when(ctx.newPromise()).thenReturn(responsePromise);
    lenient().when(responsePromise.addListener(any())).thenReturn(responsePromise);
  }

  // --- Helpers for Reflection (Given NettyContext is highly coupled/package-private) ---

  private void initContext() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, "100");
    handler.channelRead(ctx, req); // Tests createContext naturally
  }

  private void setContextField(String fieldName, Object value) throws Exception {
    Field ctxField = NettyHandler.class.getDeclaredField("context");
    ctxField.setAccessible(true);
    Object nettyCtx = ctxField.get(handler);
    if (nettyCtx != null) {
      Field f = nettyCtx.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(nettyCtx, value);
    }
  }

  private Object getContextField(String fieldName) throws Exception {
    Field ctxField = NettyHandler.class.getDeclaredField("context");
    ctxField.setAccessible(true);
    Object nettyCtx = ctxField.get(handler);
    if (nettyCtx != null) {
      Field f = nettyCtx.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      return f.get(nettyCtx);
    }
    return null;
  }

  private void setHandlerField(String fieldName, Object value) throws Exception {
    Field f = NettyHandler.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(handler, value);
  }

  private Object getHandlerField(String fieldName) throws Exception {
    Field f = NettyHandler.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(handler);
  }

  // --- HttpRequest Branch Coverage ---

  @Test
  void channelRead_HttpRequest_GET() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req =
        new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/path?q=1");

    handler.channelRead(ctx, req);

    verify(contextSelector).select("/path");
    verify(router).match(any(Context.class));
    verify(match).execute(any(Context.class));
  }

  @Test
  void channelRead_DefaultHeadersFalse() throws Exception {
    handler = new NettyHandler(serverDate, contextSelector, 1024, 10, 8192, false, false);
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

    handler.channelRead(ctx, req);
    verify(serverDate, never()).date();
  }

  @Test
  void channelRead_HttpRequest_POST_NoBody() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");

    handler.channelRead(ctx, req);

    verify(match).execute(any(Context.class));
  }

  @Test
  void channelRead_HttpRequest_POST_FullHttpRequest_TooLarge() throws Exception {
    handler.handlerAdded(ctx);
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[2048]); // > 1024 maxRequestSize
    DefaultFullHttpRequest req =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", buf);
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, 2048);

    handler.channelRead(ctx, req);

    verify(match).execute(any(Context.class), eq(Route.REQUEST_ENTITY_TOO_LARGE));
    assertEquals(0, buf.refCnt()); // Asserts release(req) triggered safely
  }

  @Test
  void channelRead_HttpRequest_POST_FullHttpRequest_Valid() throws Exception {
    handler.handlerAdded(ctx);
    ByteBuf buf = Unpooled.wrappedBuffer(new byte[500]);
    DefaultFullHttpRequest req =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/", buf);
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, 500);

    handler.channelRead(ctx, req);

    verify(match).execute(any(Context.class));
  }

  @Test
  void channelRead_HttpRequest_POST_Chunked_Multipart() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    req.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    req.headers().set(HttpHeaderNames.CONTENT_TYPE, MediaType.MULTIPART_FORMDATA);

    handler.channelRead(ctx, req);

    Object decoder = getContextField("decoder");
    assertNotNull(decoder);
    assertEquals("HttpPostMultipartRequestDecoder", decoder.getClass().getSimpleName());
  }

  @Test
  void channelRead_HttpRequest_POST_Chunked_UrlEncoded() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, "100");
    req.headers().set(HttpHeaderNames.CONTENT_TYPE, MediaType.FORM_URLENCODED);

    handler.channelRead(ctx, req);

    Object decoder = getContextField("decoder");
    assertNotNull(decoder);
    assertEquals("HttpPostStandardRequestDecoder", decoder.getClass().getSimpleName());
  }

  @Test
  void channelRead_HttpRequest_POST_Chunked_Raw() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, "100");
    req.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");

    handler.channelRead(ctx, req);

    Object decoder = getContextField("decoder");
    assertNotNull(decoder);
    assertEquals("HttpRawPostRequestDecoder", decoder.getClass().getSimpleName());
  }

  @Test
  void contentLength_Invalid_Format() throws Exception {
    handler.handlerAdded(ctx);
    DefaultHttpRequest req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    req.headers().set(HttpHeaderNames.CONTENT_LENGTH, "invalid");

    handler.channelRead(ctx, req);

    verify(match).execute(any(Context.class)); // Executes immediately (Parsed as -1)
  }

  // --- HttpContent Branch Coverage ---

  @Test
  void channelRead_HttpContent_DecoderNull() throws Exception {
    handler.handlerAdded(ctx);
    handler.channelRead(ctx, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultHttpContent chunk = new DefaultHttpContent(buf);

    handler.channelRead(ctx, chunk);

    assertEquals(0, buf.refCnt()); // Ignored but safely released
  }

  @Test
  void channelRead_HttpContent_TooLarge() throws Exception {
    initContext();

    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    DefaultHttpDataFactory factoryMock = mock(DefaultHttpDataFactory.class);

    setContextField("decoder", decoderMock);
    setContextField("httpDataFactory", factoryMock);

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[1025]); // Exceeds 1024
    DefaultHttpContent chunk = new DefaultHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(match).execute(any(Context.class), eq(Route.REQUEST_ENTITY_TOO_LARGE));
    assertEquals(0, buf.refCnt());
    verify(factoryMock).cleanAllHttpData();
    verify(decoderMock).destroy();
    assertNull(getContextField("decoder"));
  }

  @Test
  void channelRead_HttpContent_ValidChunk_NotLast() throws Exception {
    initContext();
    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    setContextField("decoder", decoderMock);

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultHttpContent chunk = new DefaultHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(decoderMock).offer(chunk);
    verify(match, never()).execute(any(Context.class));
    assertEquals(0, buf.refCnt());
  }

  @Test
  void channelRead_HttpContent_LastChunk_Matches() throws Exception {
    initContext();
    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    setContextField("decoder", decoderMock);
    when(match.matches()).thenReturn(true);

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultLastHttpContent chunk = new DefaultLastHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(decoderMock).offer(chunk);
    verify(match).execute(any(Context.class));
    assertEquals(0, buf.refCnt());
    assertNotNull(getContextField("decoder")); // Not destroyed because matches=true
  }

  @Test
  void channelRead_HttpContent_LastChunk_NoMatch() throws Exception {
    initContext();
    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    setContextField("decoder", decoderMock);
    when(match.matches()).thenReturn(false);

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultLastHttpContent chunk = new DefaultLastHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(match).execute(any(Context.class));
    assertNull(getContextField("decoder")); // Reset and destroyed safely
  }

  @Test
  void channelRead_HttpContent_OfferFails_TooManyFields() throws Exception {
    initContext();
    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    setContextField("decoder", decoderMock);

    Exception ex = new HttpPostRequestDecoder.TooManyFormFieldsException();
    doThrow(ex).when(decoderMock).offer(any(HttpContent.class));

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultHttpContent chunk = new DefaultHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(match).execute(any(Context.class), eq(Route.FORM_DECODER_HANDLER));
    assertEquals(0, buf.refCnt());
  }

  @Test
  void channelRead_HttpContent_OfferFails_GenericException() throws Exception {
    initContext();
    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    setContextField("decoder", decoderMock);

    Exception ex = new RuntimeException("Generic Error");
    doThrow(ex).when(decoderMock).offer(any(HttpContent.class));

    ByteBuf buf = Unpooled.wrappedBuffer(new byte[10]);
    DefaultHttpContent chunk = new DefaultHttpContent(buf);

    handler.channelRead(ctx, chunk);

    verify(match).execute(any(Context.class), eq(Route.FORM_DECODER_HANDLER));
  }

  // --- WebSocketFrame Branch Coverage ---

  @Test
  void channelRead_WebSocketFrame_Handled() throws Exception {
    initContext();

    Class<?> wsClass = Class.forName("io.jooby.internal.netty.NettyWebSocket");
    Object wsMock = mock(wsClass);
    setContextField("webSocket", wsMock);

    WebSocketFrame frame = new TextWebSocketFrame("test");
    handler.channelRead(ctx, frame);

    Method handleFrameMethod = wsClass.getDeclaredMethod("handleFrame", WebSocketFrame.class);
    handleFrameMethod.setAccessible(true);
    handleFrameMethod.invoke(verify(wsMock), frame);
  }

  @Test
  void channelRead_WebSocketFrame_Dropped() throws Exception {
    initContext(); // Context has no websocket

    WebSocketFrame frame = new TextWebSocketFrame("test");
    handler.channelRead(ctx, frame);

    assertEquals(0, frame.refCnt()); // Silently released
  }

  @Test
  void channelRead_UnknownMessage() {
    handler.channelRead(ctx, new Object()); // Passes cleanly, executes nothing.
  }

  // --- IO / Dispatch Write Branches ---

  @Test
  void writeMessage_InEventLoop_ReadTrue() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", true);

    ChannelPromise promise = mock(ChannelPromise.class);
    handler.writeMessage("msg", promise);

    verify(ctx).write("msg", promise);
    assertTrue((Boolean) getHandlerField("flush"));
  }

  @Test
  void writeMessage_InEventLoop_ReadFalse() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", false);

    ChannelPromise promise = mock(ChannelPromise.class);
    handler.writeMessage("msg", promise);

    verify(ctx).writeAndFlush("msg", promise);
  }

  @Test
  void writeMessage_NotInEventLoop() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(false);

    ChannelPromise promise = mock(ChannelPromise.class);
    handler.writeMessage("msg", promise);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(captor.capture());

    when(executor.inEventLoop()).thenReturn(true);
    captor.getValue().run();
    verify(ctx).writeAndFlush("msg", promise);
  }

  @Test
  void writeChunks_4args_InEventLoop_ReadTrue() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", true);
    ChannelPromise voidPromise = mock(ChannelPromise.class);
    when(ctx.voidPromise()).thenReturn(voidPromise);
    ChannelPromise promise = mock(ChannelPromise.class);

    handler.writeChunks("header", "body", "last", promise);

    verify(ctx).write("header", voidPromise);
    verify(ctx).write("body", voidPromise);
    verify(ctx).write("last", promise);
  }

  @Test
  void writeChunks_4args_InEventLoop_ReadFalse() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", false);
    ChannelPromise voidPromise = mock(ChannelPromise.class);
    when(ctx.voidPromise()).thenReturn(voidPromise);
    ChannelPromise promise = mock(ChannelPromise.class);

    handler.writeChunks("header", "body", "last", promise);

    verify(ctx).write("header", voidPromise);
    verify(ctx).write("body", voidPromise);
    verify(ctx).writeAndFlush("last", promise);
  }

  @Test
  void writeChunks_4args_NotInEventLoop() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(false);

    ChannelPromise promise = mock(ChannelPromise.class);
    handler.writeChunks("header", "body", "last", promise);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(captor.capture());

    when(executor.inEventLoop()).thenReturn(true);
    when(ctx.voidPromise()).thenReturn(mock(ChannelPromise.class));
    captor.getValue().run();

    verify(ctx).write(eq("header"), any());
  }

  @Test
  void write_Consumer_InEventLoop() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);

    SneakyThrows.Consumer<ChannelHandlerContext> consumer = mock(SneakyThrows.Consumer.class);
    handler.write(consumer);

    verify(consumer).accept(ctx);
  }

  @Test
  void write_Consumer_NotInEventLoop() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(false);

    SneakyThrows.Consumer<ChannelHandlerContext> consumer = mock(SneakyThrows.Consumer.class);
    handler.write(consumer);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(captor.capture());

    when(executor.inEventLoop()).thenReturn(true);
    captor.getValue().run();
    verify(consumer).accept(ctx);
  }

  @Test
  void writeChunks_3args_InEventLoop_ReadTrue() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", true);
    ChannelPromise voidPromise = mock(ChannelPromise.class);
    when(ctx.voidPromise()).thenReturn(voidPromise);
    ChannelPromise promise = mock(ChannelPromise.class);

    handler.writeChunks("header", "body", promise);

    verify(ctx).write("header", voidPromise);
    verify(ctx).write("body", promise);
  }

  @Test
  void writeChunks_3args_InEventLoop_ReadFalse() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(true);
    setHandlerField("read", false);
    ChannelPromise voidPromise = mock(ChannelPromise.class);
    when(ctx.voidPromise()).thenReturn(voidPromise);
    ChannelPromise promise = mock(ChannelPromise.class);

    handler.writeChunks("header", "body", promise);

    verify(ctx).write("header", voidPromise);
    verify(ctx).writeAndFlush("body", promise);
  }

  @Test
  void writeChunks_3args_NotInEventLoop() throws Exception {
    handler.handlerAdded(ctx);
    when(executor.inEventLoop()).thenReturn(false);

    ChannelPromise promise = mock(ChannelPromise.class);
    handler.writeChunks("header", "body", promise);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(captor.capture());

    when(executor.inEventLoop()).thenReturn(true);
    when(ctx.voidPromise()).thenReturn(mock(ChannelPromise.class));
    captor.getValue().run();

    verify(ctx).write(eq("header"), any());
  }

  @Test
  void channelReadComplete() throws Exception {
    setHandlerField("read", true);
    setHandlerField("flush", true);

    handler.channelReadComplete(ctx);

    verify(ctx).flush();
    assertFalse((Boolean) getHandlerField("read"));
    assertFalse((Boolean) getHandlerField("flush"));
  }

  // --- Lifecycle & Exception Coverage ---

  @Test
  @SuppressWarnings("unchecked")
  void userEventTriggered_IdleStateEvent() throws Exception {
    Attribute<Object> attr = mock(Attribute.class);
    when(channel.attr(any())).thenReturn(attr);

    Class<?> wsClass = Class.forName("io.jooby.internal.netty.NettyWebSocket");
    Object wsMock = mock(wsClass);
    when(attr.getAndSet(null)).thenReturn(wsMock);

    handler.userEventTriggered(ctx, IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);

    Method closeMethod = wsClass.getDeclaredMethod("close", io.jooby.WebSocketCloseStatus.class);
    closeMethod.setAccessible(true);
    closeMethod.invoke(verify(wsMock), io.jooby.WebSocketCloseStatus.GOING_AWAY);
  }

  @Test
  void userEventTriggered_OtherEvent() {
    handler.userEventTriggered(ctx, new Object());
    verifyNoInteractions(channel); // Fast bypass
  }

  @Test
  void exceptionCaught_ConnectionLost_NoContext() throws Exception {
    Logger mockLogger = mock(Logger.class);
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    setHandlerField("log", mockLogger);

    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      Exception cause = new Exception();
      serverMock.when(() -> Server.connectionLost(cause)).thenReturn(true);

      handler.exceptionCaught(ctx, cause);

      verify(mockLogger).debug("execution resulted in connection lost", cause);
      verify(ctx).close();
    }
  }

  @Test
  void exceptionCaught_ConnectionLost_WithContext() throws Exception {
    Logger mockLogger = mock(Logger.class);
    when(mockLogger.isDebugEnabled()).thenReturn(true);
    setHandlerField("log", mockLogger);

    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      Exception cause = new Exception();
      serverMock.when(() -> Server.connectionLost(cause)).thenReturn(true);

      initContext(); // Boots full context mapping
      handler.exceptionCaught(ctx, cause);

      verify(mockLogger).debug(eq("{} {}"), eq("POST"), eq("/"), eq(cause));
      verify(ctx).close();
    }
  }

  @Test
  void exceptionCaught_OtherError_NoContext() throws Exception {
    Logger mockLogger = mock(Logger.class);
    setHandlerField("log", mockLogger);

    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      Exception cause = new Exception();
      serverMock.when(() -> Server.connectionLost(cause)).thenReturn(false);

      handler.exceptionCaught(ctx, cause);

      verify(mockLogger).error("execution resulted in exception", cause);
      verify(ctx).close();
    }
  }

  @Test
  void exceptionCaught_OtherError_WithContext_RouterStopped() throws Exception {
    Logger mockLogger = mock(Logger.class);
    setHandlerField("log", mockLogger);

    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      Exception cause = new Exception();
      serverMock.when(() -> Server.connectionLost(cause)).thenReturn(false);

      // FIX: NettyContext relies on 'app' as its router reference.
      when(app.isStopped()).thenReturn(true);

      initContext();
      handler.exceptionCaught(ctx, cause);

      verify(mockLogger)
          .debug("execution resulted in exception while application was shutting down", cause);
      verify(ctx).close();
    }
  }

  @Test
  void exceptionCaught_OtherError_WithContext_RouterRunning() throws Exception {
    try (MockedStatic<Server> serverMock = mockStatic(Server.class)) {
      Exception cause = new Exception();
      serverMock.when(() -> Server.connectionLost(cause)).thenReturn(false);

      // FIX: NettyContext relies on 'app' as its router reference.
      when(app.isStopped()).thenReturn(false);

      initContext();
      handler.exceptionCaught(ctx, cause);

      verify(ctx).close();
    }
  }

  @Test
  void exceptionCaught_ClearsDecoderSafely() throws Exception {
    initContext();

    InterfaceHttpPostRequestDecoder decoderMock = mock(InterfaceHttpPostRequestDecoder.class);
    DefaultHttpDataFactory factoryMock = mock(DefaultHttpDataFactory.class);

    setContextField("decoder", decoderMock);
    setContextField("httpDataFactory", factoryMock);

    handler.exceptionCaught(ctx, new RuntimeException());

    verify(factoryMock).cleanAllHttpData();
    verify(decoderMock).destroy();
    verify(ctx).close();
  }

  // --- Static Utility Coverage ---

  @Test
  void testPathOnly() {
    assertEquals("/foo", NettyHandler.pathOnly("/foo?bar=1"));
    assertEquals("/foo", NettyHandler.pathOnly("/foo"));
  }
}
