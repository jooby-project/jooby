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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.rpc.grpc.GrpcProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

@ExtendWith(MockitoExtension.class)
class NettyPipelineTest {

  @Mock SslContext sslContext;
  @Mock HttpDecoderConfig decoderConfig;
  @Mock Context.Selector contextSelector;
  @Mock NettyDateService dateService;
  @Mock GrpcProcessor grpcProcessor;

  @Mock SocketChannel channel;
  @Mock ChannelPipeline pipeline;
  @Mock EventLoop eventLoop;
  @Mock ByteBufAllocator allocator;

  @BeforeEach
  void setup() {
    lenient().when(channel.pipeline()).thenReturn(pipeline);
    lenient().when(channel.eventLoop()).thenReturn(eventLoop);
    lenient().when(channel.alloc()).thenReturn(allocator);

    // FIX 1: Tell the pipeline to return the channel to prevent the NPE
    lenient().when(pipeline.channel()).thenReturn(channel);

    // Stub SslContext handler creation
    lenient().when(sslContext.newHandler(allocator)).thenReturn(mock(SslHandler.class));
  }

  // --- HTTP/1.1 Configurations ---

  @Test
  void shouldConfigureHttp11_AllHandlersEnabled() {
    // FIX 2: Changed maxRequestSize from 1024 to 16384 (16KB) to satisfy Netty's HTTP/2 spec
    // requirements
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            false,
            true,
            6,
            dateService,
            grpcProcessor);

    nettyPipeline.initChannel(channel);

    verify(pipeline, never()).addLast(eq("ssl"), any());
    verify(pipeline).addLast(eq("codec"), any(NettyServerCodec.class));
    verify(pipeline).addLast(eq("expect-continue"), any(HttpServerExpectContinueHandler.class));
    verify(pipeline).addLast(eq("compressor"), any(HttpChunkContentCompressor.class));
    verify(pipeline).addLast(eq("ws-compressor"), any(NettyWebSocketCompressor.class));
    verify(pipeline).addLast(eq("grpc"), any(NettyGrpcHandler.class));
    verify(pipeline).addLast(eq("handler"), any(NettyHandler.class));
  }

  @Test
  void shouldConfigureHttp11_MinimalHandlers() {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            false,
            false,
            null,
            dateService,
            null);

    nettyPipeline.initChannel(channel);

    verify(pipeline, never()).addLast(eq("expect-continue"), any());
    verify(pipeline, never()).addLast(eq("compressor"), any());
    verify(pipeline, never()).addLast(eq("ws-compressor"), any());
    verify(pipeline, never()).addLast(eq("grpc"), any());
  }

  // --- HTTP/2 Secure (ALPN) ---

  @Test
  void shouldConfigureHttp2Secure_AlpnHandshake() throws Exception {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            sslContext,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            true,
            false,
            null,
            dateService,
            null);

    nettyPipeline.initChannel(channel);

    verify(pipeline).addLast(eq("ssl"), any(SslHandler.class));

    ArgumentCaptor<ChannelHandler> captor = ArgumentCaptor.forClass(ChannelHandler.class);
    verify(pipeline).addLast(eq("h2-handshake"), captor.capture());

    ApplicationProtocolNegotiationHandler alpnHandler =
        (ApplicationProtocolNegotiationHandler) captor.getValue();

    Method configurePipeline =
        ApplicationProtocolNegotiationHandler.class.getDeclaredMethod(
            "configurePipeline", ChannelHandlerContext.class, String.class);
    configurePipeline.setAccessible(true);

    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    when(ctx.pipeline()).thenReturn(pipeline);

    // Branch 1: Protocol is HTTP/2
    configurePipeline.invoke(alpnHandler, ctx, ApplicationProtocolNames.HTTP_2);
    verify(pipeline).addLast(eq("http2-codec"), any());
    verify(pipeline).addLast(eq("http2-multiplex"), any(Http2MultiplexHandler.class));

    // Branch 2: Protocol is HTTP/1.1
    configurePipeline.invoke(alpnHandler, ctx, ApplicationProtocolNames.HTTP_1_1);
    verify(pipeline).addLast(eq("codec"), any(NettyServerCodec.class));
  }

  // --- HTTP/2 Cleartext (Preface or Upgrade) ---

  @Test
  void shouldConfigureHttp2Cleartext_DecodePrefaceOrUpgrade() throws Exception {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            true,
            true,
            6,
            dateService,
            grpcProcessor);

    nettyPipeline.initChannel(channel);

    ArgumentCaptor<ChannelHandler> captor = ArgumentCaptor.forClass(ChannelHandler.class);
    verify(pipeline).addLast(eq("h2-handshake"), captor.capture());

    ByteToMessageDecoder prefaceHandler = (ByteToMessageDecoder) captor.getValue();

    Method decode =
        ByteToMessageDecoder.class.getDeclaredMethod(
            "decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
    decode.setAccessible(true);

    ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    when(ctx.pipeline()).thenReturn(pipeline);
    List<Object> out = new ArrayList<>();

    // Branch 1: Not enough bytes (returns early)
    ByteBuf shortBuf = Unpooled.wrappedBuffer(new byte[] {1, 2});
    decode.invoke(prefaceHandler, ctx, shortBuf, out);
    verify(pipeline, never()).addLast(eq("http2-codec"), any());

    // Branch 2: Matches "PRI " (HTTP/2 Prior Knowledge)
    ByteBuf priBuf = Unpooled.buffer().writeInt(0x50524920); // "PRI "
    decode.invoke(prefaceHandler, ctx, priBuf, out);
    verify(pipeline).addLast(eq("http2-codec"), any());
    verify(pipeline).remove(prefaceHandler);

    // Branch 3: Doesn't match "PRI " (HTTP/1.1 Cleartext Upgrade)
    ByteBuf getBuf = Unpooled.buffer().writeInt(0x47455420); // "GET "
    decode.invoke(prefaceHandler, ctx, getBuf, out);
    verify(pipeline).addLast(eq("h2upgrade"), any(HttpServerUpgradeHandler.class));
  }

  // --- Upgrade Cleaner & UpgradeCodecFactory Logic ---

  @Test
  void testHttp11UpgradeCleanerAndCodecFactory() throws Exception {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            true,
            true,
            6,
            dateService,
            grpcProcessor);

    // Trigger HTTP/1.1 Upgrade setup via reflection on the private method
    Method setupHttp11Upgrade =
        NettyPipeline.class.getDeclaredMethod("setupHttp11Upgrade", ChannelPipeline.class);
    setupHttp11Upgrade.setAccessible(true);
    setupHttp11Upgrade.invoke(nettyPipeline, pipeline);

    // 1. Test the UpgradeCodecFactory Lambda
    ArgumentCaptor<ChannelHandler> upgradeCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
    verify(pipeline).addLast(eq("h2upgrade"), upgradeCaptor.capture());
    HttpServerUpgradeHandler upgradeHandler = (HttpServerUpgradeHandler) upgradeCaptor.getValue();

    Field factoryField = HttpServerUpgradeHandler.class.getDeclaredField("upgradeCodecFactory");
    factoryField.setAccessible(true);
    HttpServerUpgradeHandler.UpgradeCodecFactory factory =
        (HttpServerUpgradeHandler.UpgradeCodecFactory) factoryField.get(upgradeHandler);

    assertNotNull(factory.newUpgradeCodec("h2c")); // Match
    assertNull(factory.newUpgradeCodec("http/1.1")); // No match

    // 2. Test the h2upgrade-cleaner userEventTriggered
    ArgumentCaptor<ChannelHandler> cleanerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
    verify(pipeline).addLast(eq("h2upgrade-cleaner"), cleanerCaptor.capture());
    ChannelInboundHandlerAdapter cleaner = (ChannelInboundHandlerAdapter) cleanerCaptor.getValue();

    ChannelHandlerContext cleanerCtx = mock(ChannelHandlerContext.class);
    when(cleanerCtx.pipeline()).thenReturn(pipeline);

    // Mock the pipeline context returns so it attempts to remove them
    when(pipeline.context("grpc")).thenReturn(mock(ChannelHandlerContext.class));
    when(pipeline.context("handler")).thenReturn(mock(ChannelHandlerContext.class));
    when(pipeline.context("expect-continue")).thenReturn(mock(ChannelHandlerContext.class));
    when(pipeline.context("compressor"))
        .thenReturn(null); // Leave null to cover both null/non-null branches
    when(pipeline.context("ws-compressor")).thenReturn(mock(ChannelHandlerContext.class));

    // Bypass Netty's package-private constructor for UpgradeEvent
    Constructor<HttpServerUpgradeHandler.UpgradeEvent> eventConstructor =
        HttpServerUpgradeHandler.UpgradeEvent.class.getDeclaredConstructor(
            CharSequence.class, FullHttpRequest.class);
    eventConstructor.setAccessible(true);
    HttpServerUpgradeHandler.UpgradeEvent upgradeEvent =
        eventConstructor.newInstance("h2c", mock(FullHttpRequest.class));

    // Fire the upgrade event
    cleaner.userEventTriggered(cleanerCtx, upgradeEvent);

    // Assert Removals
    verify(pipeline).remove("grpc");
    verify(pipeline).remove("handler");
    verify(pipeline).remove("expect-continue");
    verify(pipeline, never()).remove("compressor"); // Skipped because context() was null
    verify(pipeline).remove("ws-compressor");
    verify(pipeline).remove(cleaner); // Self destructs

    // Fire a generic object event (covers super call fallback branch)
    cleaner.userEventTriggered(cleanerCtx, new Object());
  }

  // --- Multiplexed HTTP/2 Stream Initializer ---

  @Test
  void testHttp2StreamInitializer_WithGrpc() throws Exception {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            true,
            true,
            6,
            dateService,
            grpcProcessor);

    // Instantiate Http2StreamInitializer
    Class<?> initClass =
        Class.forName("io.jooby.internal.netty.NettyPipeline$Http2StreamInitializer");
    Constructor<?> constructor = initClass.getDeclaredConstructors()[0];
    constructor.setAccessible(true);

    @SuppressWarnings("unchecked")
    ChannelInitializer<Channel> initializer =
        (ChannelInitializer<Channel>) constructor.newInstance(nettyPipeline);

    Channel childChannel = mock(Channel.class);
    ChannelPipeline childPipeline = mock(ChannelPipeline.class);
    when(childChannel.pipeline()).thenReturn(childPipeline);
    when(childChannel.eventLoop()).thenReturn(eventLoop);

    Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
    initChannel.setAccessible(true);

    initChannel.invoke(initializer, childChannel);

    verify(childPipeline).addLast(eq("http2"), any(Http2StreamFrameToHttpObjectCodec.class));
    verify(childPipeline)
        .addLast(eq("grpc"), any(NettyGrpcHandler.class)); // Added because GrpcProcessor != null
    verify(childPipeline).addLast(eq("handler"), any(NettyHandler.class));
  }

  @Test
  void testHttp2StreamInitializer_WithoutGrpc() throws Exception {
    NettyPipeline nettyPipeline =
        new NettyPipeline(
            null,
            decoderConfig,
            contextSelector,
            16384,
            10,
            8192,
            true,
            true,
            true,
            6,
            dateService,
            null // Null GrpcProcessor
            );

    Class<?> initClass =
        Class.forName("io.jooby.internal.netty.NettyPipeline$Http2StreamInitializer");
    Constructor<?> constructor = initClass.getDeclaredConstructors()[0];
    constructor.setAccessible(true);

    @SuppressWarnings("unchecked")
    ChannelInitializer<Channel> initializer =
        (ChannelInitializer<Channel>) constructor.newInstance(nettyPipeline);

    Channel childChannel = mock(Channel.class);
    ChannelPipeline childPipeline = mock(ChannelPipeline.class);
    when(childChannel.pipeline()).thenReturn(childPipeline);
    when(childChannel.eventLoop()).thenReturn(eventLoop);

    Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
    initChannel.setAccessible(true);

    initChannel.invoke(initializer, childChannel);

    verify(childPipeline, never()).addLast(eq("grpc"), any()); // Skipped
    verify(childPipeline).addLast(eq("handler"), any(NettyHandler.class));
  }
}
