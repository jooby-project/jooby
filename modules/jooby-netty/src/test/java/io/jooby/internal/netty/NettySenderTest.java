/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Sender;
import io.jooby.output.Output;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

@ExtendWith(MockitoExtension.class)
class NettySenderTest {

  @Mock NettyContext ctx;
  @Mock ChannelHandlerContext channelContext;
  @Mock ChannelFuture channelFuture;
  @Mock Sender.Callback callback;

  private NettySender sender;

  @BeforeEach
  void setup() {
    // NettySender accesses the package-private ctx field directly
    ctx.ctx = channelContext;
    sender = new NettySender(ctx);
  }

  @Test
  void testWriteByteArray() {
    when(channelContext.writeAndFlush(any(DefaultHttpContent.class))).thenReturn(channelFuture);

    byte[] data = {1, 2, 3};
    sender.write(data, callback);

    verify(channelContext).writeAndFlush(any(DefaultHttpContent.class));
    verify(channelFuture).addListener(any(ChannelFutureListener.class));
  }

  @Test
  void testWriteOutput() {
    when(channelContext.writeAndFlush(any(DefaultHttpContent.class))).thenReturn(channelFuture);
    Output output = mock(Output.class);

    // Mock the static NettyByteBufRef.byteBuf helper
    try (MockedStatic<NettyByteBufRef> bufRefMock = mockStatic(NettyByteBufRef.class)) {
      bufRefMock
          .when(() -> NettyByteBufRef.byteBuf(output))
          .thenReturn(Unpooled.wrappedBuffer(new byte[] {1}));

      sender.write(output, callback);

      verify(channelContext).writeAndFlush(any(DefaultHttpContent.class));
      verify(channelFuture).addListener(any(ChannelFutureListener.class));
    }
  }

  @Test
  void testClose() {
    ChannelPromise promise = mock(ChannelPromise.class);
    when(ctx.promise()).thenReturn(promise);

    sender.close();

    verify(channelContext).writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise);
    verify(ctx).requestComplete();
  }

  @Test
  void testListenerSuccess() throws Exception {
    when(channelContext.writeAndFlush(any())).thenReturn(channelFuture);
    sender.write(new byte[0], callback);

    // Capture the listener lambda
    ArgumentCaptor<ChannelFutureListener> captor =
        ArgumentCaptor.forClass(ChannelFutureListener.class);
    verify(channelFuture).addListener(captor.capture());
    ChannelFutureListener listener = captor.getValue();

    // Trigger Success Branch
    when(channelFuture.isSuccess()).thenReturn(true);
    listener.operationComplete(channelFuture);

    verify(callback).onComplete(ctx, null);
  }

  @Test
  void testListenerFailure() throws Exception {
    when(channelContext.writeAndFlush(any())).thenReturn(channelFuture);
    sender.write(new byte[0], callback);

    // Capture the listener lambda
    ArgumentCaptor<ChannelFutureListener> captor =
        ArgumentCaptor.forClass(ChannelFutureListener.class);
    verify(channelFuture).addListener(captor.capture());
    ChannelFutureListener listener = captor.getValue();

    // Trigger Failure Branch
    Exception cause = new Exception("Connection lost");
    when(channelFuture.isSuccess()).thenReturn(false);
    when(channelFuture.cause()).thenReturn(cause);

    listener.operationComplete(channelFuture);

    verify(callback).onComplete(ctx, cause);
    verify(ctx).log(cause);
  }

  @Test
  void testListenerFailure_CallbackThrowsException_TriggersFinallyBlock() throws Exception {
    when(channelContext.writeAndFlush(any())).thenReturn(channelFuture);
    sender.write(new byte[0], callback);

    ArgumentCaptor<ChannelFutureListener> captor =
        ArgumentCaptor.forClass(ChannelFutureListener.class);
    verify(channelFuture).addListener(captor.capture());
    ChannelFutureListener listener = captor.getValue();

    Exception cause = new Exception("Connection lost");
    when(channelFuture.isSuccess()).thenReturn(false);
    when(channelFuture.cause()).thenReturn(cause);

    // Simulate the callback throwing an exception to ensure ctx.log(cause) is still executed
    doThrow(new RuntimeException("Callback Crash")).when(callback).onComplete(ctx, cause);

    assertThrows(RuntimeException.class, () -> listener.operationComplete(channelFuture));

    // Asserts the finally block correctly executes despite the callback crash
    verify(ctx).log(cause);
  }
}
