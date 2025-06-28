/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

public class Issue3554 {

  @Test
  public void shouldCloseOutputStreamOnce() throws IOException {
    var ctx = mock(NettyContext.class);
    var headers = mock(HttpResponse.class);

    var buffer = mock(ByteBuf.class);
    when(buffer.readableBytes()).thenReturn(0);

    var bufferAllocator = mock(ByteBufAllocator.class);
    when(bufferAllocator.heapBuffer(1024, 1024)).thenReturn(buffer);

    var future = mock(ChannelFuture.class);
    var channelContext = mock(ChannelHandlerContext.class);
    when(channelContext.alloc()).thenReturn(bufferAllocator);
    when(channelContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).thenReturn(future);

    var httpOutputStream = new NettyOutputStream(ctx, channelContext, 1024, headers);
    httpOutputStream.close();
    httpOutputStream.close();

    verify(channelContext, times(1)).writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
  }
}
