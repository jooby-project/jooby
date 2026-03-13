/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.jooby.rpc.grpc.GrpcExchange;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyGrpcExchange implements GrpcExchange {

  private final ChannelHandlerContext ctx;
  private final HttpRequest request;
  private boolean headersSent = false;

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
    if (!headersSent) {
      // Send the initial HTTP/2 HEADERS frame (Status 200)
      HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/grpc");
      ctx.write(response);
      headersSent = true;
    }
  }

  @Override
  public void send(ByteBuffer payload, Consumer<Throwable> callback) {
    sendHeadersIfNecessary();

    // Wrap the NIO ByteBuffer in a Netty ByteBuf without copying
    HttpContent chunk = new DefaultHttpContent(Unpooled.wrappedBuffer(payload));

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
    if (headersSent) {
      // Trailers-Appended: Send the final HTTP/2 HEADERS frame with END_STREAM flag
      LastHttpContent lastContent = new DefaultLastHttpContent();
      lastContent.trailingHeaders().set("grpc-status", String.valueOf(statusCode));
      if (description != null) {
        lastContent.trailingHeaders().set("grpc-message", description);
      }
      // writeAndFlush the LastHttpContent, then close the Netty stream channel
      ctx.writeAndFlush(lastContent).addListener(ChannelFutureListener.CLOSE);
    } else {
      // Trailers-Only: No body was sent, so standard headers become the trailers
      HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/grpc");
      response.headers().set("grpc-status", String.valueOf(statusCode));
      if (description != null) {
        response.headers().set("grpc-message", description);
      }
      ctx.write(response);

      // Close out the stream with an empty DATA frame possessing the END_STREAM flag
      ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
          .addListener(ChannelFutureListener.CLOSE);
    }
  }
}
