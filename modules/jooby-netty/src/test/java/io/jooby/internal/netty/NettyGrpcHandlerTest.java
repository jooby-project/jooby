/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.rpc.grpc.GrpcProcessor;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

@ExtendWith(MockitoExtension.class)
class NettyGrpcHandlerTest {

  @Mock GrpcProcessor processor;
  @Mock ChannelHandlerContext ctx;
  @Mock HttpRequest request;
  @Mock HttpHeaders headers;

  @BeforeEach
  void setup() {
    lenient().when(request.headers()).thenReturn(headers);
  }

  private void setInternalState(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  void testChannelRead_Http1_RejectsWithUpgradeRequired() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, false); // isHttp2 = false

    when(request.uri()).thenReturn("/io.grpc.Service/Method");
    when(headers.get(HttpHeaderNames.CONTENT_TYPE)).thenReturn("application/grpc");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);

    ChannelFuture closeFuture = mock(ChannelFuture.class);
    when(ctx.writeAndFlush(any())).thenReturn(closeFuture);

    try (MockedStatic<ReferenceCountUtil> rcu = mockStatic(ReferenceCountUtil.class)) {
      handler.channelRead(ctx, request);

      ArgumentCaptor<DefaultFullHttpResponse> responseCaptor =
          ArgumentCaptor.forClass(DefaultFullHttpResponse.class);
      verify(ctx).writeAndFlush(responseCaptor.capture());

      DefaultFullHttpResponse response = responseCaptor.getValue();
      assertEquals(HttpVersion.HTTP_1_1, response.protocolVersion());
      assertEquals(HttpResponseStatus.UPGRADE_REQUIRED, response.status());
      assertEquals("upgrade", response.headers().get(HttpHeaderNames.CONNECTION).toLowerCase());
      assertEquals("h2c", response.headers().get(HttpHeaderNames.UPGRADE));

      verify(closeFuture).addListener(ChannelFutureListener.CLOSE);
      rcu.verify(() -> ReferenceCountUtil.release(request));
    }
  }

  @Test
  void testChannelRead_Http2_AcceptsAndStartsBridge() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    // Tests query string extraction behavior alongside success case
    when(request.uri()).thenReturn("/io.grpc.Service/Method?param=ignore");
    when(headers.get(HttpHeaderNames.CONTENT_TYPE)).thenReturn("application/grpc+proto");
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);

    try (MockedConstruction<NettyGrpcInputBridge> bridgeConstructor =
            mockConstruction(NettyGrpcInputBridge.class);
        MockedStatic<ReferenceCountUtil> rcu = mockStatic(ReferenceCountUtil.class)) {

      handler.channelRead(ctx, request);

      // Verify the exchange was constructed and the processor consumed it
      verify(processor).process(any(NettyGrpcExchange.class));

      // Verify the input bridge was started successfully
      assertEquals(1, bridgeConstructor.constructed().size());
      verify(bridgeConstructor.constructed().get(0)).start();

      // Ensure headers request was released after setup
      rcu.verify(() -> ReferenceCountUtil.release(request));
    }
  }

  @Test
  void testChannelRead_NotGrpcMethod_PassesDownPipeline() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    when(request.uri()).thenReturn("/api/rest");
    when(headers.get(HttpHeaderNames.CONTENT_TYPE)).thenReturn("application/json");
    when(processor.isGrpcMethod("/api/rest")).thenReturn(false);

    handler.channelRead(ctx, request);

    // Bypasses interceptor and triggers super.channelRead (which calls fireChannelRead)
    verify(ctx).fireChannelRead(request);
  }

  @Test
  void testChannelRead_MissingContentType_PassesDownPipeline() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    when(request.uri()).thenReturn("/io.grpc.Service/Method");
    when(headers.get(HttpHeaderNames.CONTENT_TYPE)).thenReturn(null);
    when(processor.isGrpcMethod("/io.grpc.Service/Method")).thenReturn(true);

    handler.channelRead(ctx, request);

    verify(ctx).fireChannelRead(request);
  }

  @Test
  void testChannelRead_HttpContent_DelegatesToBridge() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    // Simulate an already-accepted gRPC stream
    setInternalState(handler, "isGrpc", true);
    NettyGrpcInputBridge mockBridge = mock(NettyGrpcInputBridge.class);
    setInternalState(handler, "inputBridge", mockBridge);

    HttpContent chunk = mock(HttpContent.class);

    try (MockedStatic<ReferenceCountUtil> rcu = mockStatic(ReferenceCountUtil.class)) {
      handler.channelRead(ctx, chunk);

      verify(mockBridge).onChunk(chunk);
      rcu.verify(() -> ReferenceCountUtil.release(chunk));
    }
  }

  @Test
  void testChannelRead_HttpContent_NullBridgeSafety() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    // Simulate an accepted gRPC stream where bridge creation failed or isn't initialized
    setInternalState(handler, "isGrpc", true);
    setInternalState(handler, "inputBridge", null);

    HttpContent chunk = mock(HttpContent.class);

    try (MockedStatic<ReferenceCountUtil> rcu = mockStatic(ReferenceCountUtil.class)) {
      handler.channelRead(ctx, chunk);

      // Ensures the chunk is safely released even if bridge routing fails
      rcu.verify(() -> ReferenceCountUtil.release(chunk));
    }
  }

  @Test
  void testChannelRead_OtherMessageType_PassesDownPipeline() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);
    Object randomMessage = new Object();

    handler.channelRead(ctx, randomMessage);

    verify(ctx).fireChannelRead(randomMessage);
  }

  @Test
  void testChannelInactive_ActiveBridge_CancelsBridge() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);

    setInternalState(handler, "isGrpc", true);
    NettyGrpcInputBridge mockBridge = mock(NettyGrpcInputBridge.class);
    setInternalState(handler, "inputBridge", mockBridge);

    handler.channelInactive(ctx);

    verify(mockBridge).cancel();
    // Verify super.channelInactive is still called
    verify(ctx).fireChannelInactive();
  }

  @Test
  void testChannelInactive_NoBridge_PassesDownPipeline() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);
    setInternalState(handler, "isGrpc", false);

    handler.channelInactive(ctx);

    verify(ctx).fireChannelInactive();
  }

  @Test
  void testExceptionCaught_ActiveGrpcStream_ClosesContext() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);
    setInternalState(handler, "isGrpc", true);
    Exception ex = new Exception("Mock gRPC Exception");

    handler.exceptionCaught(ctx, ex);

    verify(ctx).close();
    verify(ctx, never()).fireExceptionCaught(any());
  }

  @Test
  void testExceptionCaught_NotGrpcStream_PassesDownPipeline() throws Exception {
    NettyGrpcHandler handler = new NettyGrpcHandler(processor, true);
    setInternalState(handler, "isGrpc", false);
    Exception ex = new Exception("Mock General Exception");

    handler.exceptionCaught(ctx, ex);

    verify(ctx).fireExceptionCaught(ex);
    verify(ctx, never()).close();
  }
}
