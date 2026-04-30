/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

@ExtendWith(MockitoExtension.class)
class NettyGrpcExchangeTest {

  @Mock ChannelHandlerContext ctx;
  @Mock HttpRequest request;
  @Mock ChannelFuture channelFuture;

  private DefaultHttpHeaders headers;
  private NettyGrpcExchange exchange;

  @BeforeEach
  void setup() {
    headers = new DefaultHttpHeaders();
    // Use a real DefaultHttpHeaders instead of a mock to easily test iteration
    lenient().when(request.headers()).thenReturn(headers);
    exchange = new NettyGrpcExchange(ctx, request);
  }

  @Test
  void testGetRequestPath_WithoutQueryString() {
    when(request.uri()).thenReturn("/io.grpc.Service/Method");
    assertEquals("/io.grpc.Service/Method", exchange.getRequestPath());
  }

  @Test
  void testGetRequestPath_WithQueryString() {
    when(request.uri()).thenReturn("/io.grpc.Service/Method?param=value");
    assertEquals("/io.grpc.Service/Method", exchange.getRequestPath());
  }

  @Test
  void testGetHeader() {
    headers.add("User-Agent", "grpc-java");
    assertEquals("grpc-java", exchange.getHeader("User-Agent"));
    assertNull(exchange.getHeader("Missing"));
  }

  @Test
  void testGetHeaders() {
    headers.add("Content-Type", "application/grpc");
    headers.add("te", "trailers");

    Map<String, String> map = exchange.getHeaders();
    assertEquals(2, map.size());
    assertEquals("application/grpc", map.get("Content-Type"));
    assertEquals("trailers", map.get("te"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_FirstTimeSendsHeaders_AndListenerSuccess() throws Exception {
    when(ctx.writeAndFlush(any(DefaultHttpContent.class))).thenReturn(channelFuture);
    Consumer<Throwable> callback = mock(Consumer.class);

    ByteBuffer payload = ByteBuffer.wrap(new byte[] {1, 2, 3});
    exchange.send(payload, callback);

    // Verify initial headers were sent
    ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    verify(ctx).write(responseCaptor.capture());
    assertEquals("application/grpc", responseCaptor.getValue().headers().get("Content-Type"));

    // Verify payload chunk was sent
    verify(ctx).writeAndFlush(any(DefaultHttpContent.class));

    // Verify listener trigger success
    ArgumentCaptor<io.netty.util.concurrent.GenericFutureListener> listenerCaptor =
        ArgumentCaptor.forClass(io.netty.util.concurrent.GenericFutureListener.class);
    verify(channelFuture).addListener(listenerCaptor.capture());

    when(channelFuture.isSuccess()).thenReturn(true);
    listenerCaptor.getValue().operationComplete(channelFuture);

    verify(callback).accept(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSend_SubsequentSendsBypassHeaders_AndListenerFailure() throws Exception {
    when(ctx.writeAndFlush(any(DefaultHttpContent.class))).thenReturn(channelFuture);
    Consumer<Throwable> callback = mock(Consumer.class);

    // First send to toggle the AtomicBoolean
    exchange.send(ByteBuffer.allocate(0), mock(Consumer.class));
    // Second send should bypass ctx.write(response)
    exchange.send(ByteBuffer.allocate(0), callback);

    // ctx.write(response) should only be called ONCE from the first send
    verify(ctx, times(1)).write(any(HttpResponse.class));

    // Listeners are added for both sends
    ArgumentCaptor<io.netty.util.concurrent.GenericFutureListener> listenerCaptor =
        ArgumentCaptor.forClass(io.netty.util.concurrent.GenericFutureListener.class);
    verify(channelFuture, times(2)).addListener(listenerCaptor.capture());

    // Trigger failure on the second listener
    Exception cause = new Exception("Connection closed");
    when(channelFuture.isSuccess()).thenReturn(false);
    when(channelFuture.cause()).thenReturn(cause);
    listenerCaptor.getAllValues().get(1).operationComplete(channelFuture);

    verify(callback).accept(cause);
  }

  @Test
  void testClose_HeadersAlreadySent_NoDescription() {
    when(ctx.writeAndFlush(any())).thenReturn(channelFuture);

    // Trigger headers
    exchange.send(ByteBuffer.allocate(0), mock(Consumer.class));

    exchange.close(0, null);

    ArgumentCaptor<io.netty.handler.codec.http.HttpContent> captor =
        ArgumentCaptor.forClass(io.netty.handler.codec.http.HttpContent.class);

    // index 0 is the DefaultHttpContent payload from send(), index 1 is the LastHttpContent from
    // close()
    verify(ctx, times(2)).writeAndFlush(captor.capture());

    // Cast the second captured argument to LastHttpContent
    LastHttpContent lastContent = (LastHttpContent) captor.getAllValues().get(1);
    io.netty.handler.codec.http.HttpHeaders trailers = lastContent.trailingHeaders();

    assertEquals("0", trailers.get("grpc-status"));
    assertNull(trailers.get("grpc-message"));
    verify(channelFuture).addListener(ChannelFutureListener.CLOSE);
  }

  @Test
  void testClose_HeadersAlreadySent_WithDescription() {
    when(ctx.writeAndFlush(any())).thenReturn(channelFuture);

    // Trigger headers
    exchange.send(ByteBuffer.allocate(0), mock(Consumer.class));

    // Spaces should become "%20", not "+"
    exchange.close(1, "Not Found Test");

    // FIX: Capture the parent interface 'HttpContent'
    ArgumentCaptor<io.netty.handler.codec.http.HttpContent> captor =
        ArgumentCaptor.forClass(io.netty.handler.codec.http.HttpContent.class);

    verify(ctx, times(2)).writeAndFlush(captor.capture());

    // Cast the second captured argument to LastHttpContent
    LastHttpContent lastContent = (LastHttpContent) captor.getAllValues().get(1);
    io.netty.handler.codec.http.HttpHeaders trailers = lastContent.trailingHeaders();

    assertEquals("1", trailers.get("grpc-status"));
    assertEquals("Not%20Found%20Test", trailers.get("grpc-message"));
  }

  @Test
  void testClose_TrailersOnly_NoDescription() {
    when(ctx.writeAndFlush(any())).thenReturn(channelFuture);

    // Call close immediately without calling send()
    exchange.close(0, null);

    ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    verify(ctx).write(responseCaptor.capture());

    HttpResponse response = responseCaptor.getValue();
    assertEquals("application/grpc", response.headers().get("Content-Type"));
    assertEquals("0", response.headers().get("grpc-status"));
    assertNull(response.headers().get("grpc-message"));

    verify(ctx).writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    verify(channelFuture).addListener(ChannelFutureListener.CLOSE);
  }

  @Test
  void testClose_TrailersOnly_WithDescriptionEncoding() {
    when(ctx.writeAndFlush(any())).thenReturn(channelFuture);

    // Includes spaces and characters that trigger URLEncoding
    exchange.close(2, "Invalid token!");

    ArgumentCaptor<HttpResponse> responseCaptor = ArgumentCaptor.forClass(HttpResponse.class);
    verify(ctx).write(responseCaptor.capture());

    HttpResponse response = responseCaptor.getValue();
    assertEquals("2", response.headers().get("grpc-status"));

    // URLEncoder encodes '!' as '%21' and spaces as '+', our fix converts '+' to '%20'
    assertEquals("Invalid%20token%21", response.headers().get("grpc-message"));

    verify(ctx).writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
  }
}
