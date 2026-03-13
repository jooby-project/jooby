/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.jooby.rpc.grpc.GrpcExchange;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyGrpcExchange implements GrpcExchange {

  private final ChannelHandlerContext ctx;
  private final HttpRequest request;

  private final AtomicBoolean headersSent = new AtomicBoolean(false);

  public NettyGrpcExchange(ChannelHandlerContext ctx, HttpRequest request) {
    this.ctx = ctx;
    this.request = request;
  }

  @Override
  public String getRequestPath() {
    String uri = request.uri();
    int queryIndex = uri.indexOf('?');
    return queryIndex > 0 ? uri.substring(0, queryIndex) : uri;
  }

  @Override
  public String getHeader(String name) {
    return request.headers().get(name);
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : request.headers()) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

  private void sendHeadersIfNecessary() {
    if (headersSent.compareAndSet(false, true)) {
      // Send the initial HTTP/2 HEADERS frame (Status 200)
      HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/grpc");
      ctx.write(response);
    }
  }

  @Override
  public void send(ByteBuffer payload, Consumer<Throwable> callback) {
    sendHeadersIfNecessary();

    // Wrap the NIO ByteBuffer in a Netty ByteBuf without copying
    var chunk = new DefaultHttpContent(Unpooled.wrappedBuffer(payload));

    // Write and flush, then map Netty's Future to your single-lambda callback
    ctx.writeAndFlush(chunk)
        .addListener(
            future -> {
              if (future.isSuccess()) {
                callback.accept(null);
              } else {
                callback.accept(future.cause());
              }
            });
  }

  @Override
  public void close(int statusCode, String description) {
    var encodedDescription = encodeGrpcMessage(description);

    // If headers were already sent, we just need to send the final trailers
    if (headersSent.get()) {
      LastHttpContent lastContent = new DefaultLastHttpContent();
      lastContent.trailingHeaders().set("grpc-status", String.valueOf(statusCode));

      if (encodedDescription != null) {
        lastContent.trailingHeaders().set("grpc-message", encodedDescription);
      }

      ctx.writeAndFlush(lastContent).addListener(ChannelFutureListener.CLOSE);

    } else {
      // Trailers-Only fast path: Headers and trailers combined
      var response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/grpc");
      response.headers().set("grpc-status", String.valueOf(statusCode));

      if (encodedDescription != null) {
        response.headers().set("grpc-message", encodedDescription);
      }

      ctx.write(response);
      ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
          .addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** gRPC specification requires the grpc-message trailer to be percent-encoded. */
  private static String encodeGrpcMessage(String description) {
    if (description == null) {
      return null;
    }
    // URLEncoder uses '+' for spaces, but gRPC strictly expects '%20'
    return URLEncoder.encode(description, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
