/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Router;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

class NettyOutputStaticTest {

  @Test
  void shouldCalculateSizeAndWrapByteBuf() {
    ByteBuffer buffer = ByteBuffer.wrap("Jooby".getBytes(StandardCharsets.UTF_8));
    NettyOutputStatic output = new NettyOutputStatic(buffer);

    // Tests size() natively and during NettyString construction
    assertEquals(5, output.size());

    // Tests byteBuf()
    ByteBuf byteBuf = output.byteBuf();
    assertNotNull(byteBuf);
    assertEquals(5, byteBuf.readableBytes());
  }

  @Test
  void shouldSendFastPathWithRealNettyContext() {
    // We MUST use a real NettyContext here, because NettyOutputStatic strictly checks:
    // if (ctx.getClass() == NettyContext.class)
    // If we mock NettyContext, its class becomes NettyContext$MockitoMock$xxx and bypasses the fast
    // path.

    NettyHandler connection = mock(NettyHandler.class);
    ChannelHandlerContext channelCtx = mock(ChannelHandlerContext.class);

    // Prevent Netty NPEs when it registers promise lifecycle listeners
    ChannelPromise promise = mock(ChannelPromise.class);
    when(channelCtx.newPromise()).thenReturn(promise);
    when(channelCtx.voidPromise()).thenReturn(promise);
    when(promise.addListener(any())).thenReturn(promise);

    HttpRequest req = mock(HttpRequest.class);
    when(req.method()).thenReturn(HttpMethod.GET);
    when(req.headers()).thenReturn(mock(HttpHeaders.class));
    when(req.protocolVersion()).thenReturn(io.netty.handler.codec.http.HttpVersion.HTTP_1_1);

    Router router = mock(Router.class);

    // Booting the real context with safe dummy variables
    NettyContext realNettyContext =
        new NettyContext(connection, channelCtx, req, router, "/", 10, false);

    ByteBuffer buffer = ByteBuffer.wrap("Jooby Fast Path".getBytes(StandardCharsets.UTF_8));
    NettyOutputStatic output = new NettyOutputStatic(buffer);

    // Execute the target method
    output.send(realNettyContext);

    // Verify the fast path was executed by observing the side-effect sent directly to the Netty
    // connection handler
    verify(connection).writeMessage(any(DefaultFullHttpResponse.class), any());
  }

  @Test
  void shouldSendSlowPathWithGenericContext() {
    ByteBuffer buffer = ByteBuffer.wrap("Jooby Generic Path".getBytes(StandardCharsets.UTF_8));
    NettyOutputStatic output = new NettyOutputStatic(buffer);

    Context genericContext = mock(Context.class);

    // Execute the target method
    output.send(genericContext);

    // Because genericContext.getClass() != NettyContext.class, it correctly falls back
    // to the generic interface protocol
    verify(genericContext).send(any(ByteBuffer.class));
  }
}
