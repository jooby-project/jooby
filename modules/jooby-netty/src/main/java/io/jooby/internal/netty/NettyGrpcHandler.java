/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

import io.jooby.GrpcExchange;
import io.jooby.GrpcProcessor;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

public class NettyGrpcHandler extends ChannelInboundHandlerAdapter {

  private final GrpcProcessor processor;
  private final boolean isHttp2;

  // State for the current stream
  private boolean isGrpc = false;
  private NettyGrpcInputBridge inputBridge;

  public NettyGrpcHandler(GrpcProcessor processor, boolean isHttp2) {
    this.processor = processor;
    this.isHttp2 = isHttp2;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // 1. Intercept the initial Request headers
    if (msg instanceof HttpRequest req) {
      String contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);

      if (contentType != null && contentType.startsWith("application/grpc")) {
        isGrpc = true;

        if (!isHttp2) {
          // gRPC requires HTTP/2. Reject HTTP/1.1 calls immediately.
          var response =
              new DefaultFullHttpResponse(
                  HttpVersion.HTTP_1_1, HttpResponseStatus.UPGRADE_REQUIRED);
          response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE);
          response.headers().set(HttpHeaderNames.UPGRADE, "h2c");
          ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
          ReferenceCountUtil.release(msg);
          return;
        }

        // We will implement NettyGrpcExchange in the next step
        GrpcExchange exchange = new NettyGrpcExchange(ctx, req);
        Flow.Subscriber<ByteBuffer> subscriber = processor.process(exchange);

        if (subscriber != null) {
          inputBridge = new NettyGrpcInputBridge(ctx, subscriber);
          inputBridge.start();
        } else {
          // Exchange was rejected/closed internally by processor (e.g. Unimplemented)
        }

        ReferenceCountUtil.release(msg); // We consumed the headers
        return;
      }
    }

    // 2. Intercept subsequent body chunks for this gRPC stream
    if (isGrpc && msg instanceof HttpContent chunk) {
      try {
        if (inputBridge != null) {
          inputBridge.onChunk(chunk);
        }
      } finally {
        // Always release Netty's direct memory buffers
        ReferenceCountUtil.release(chunk);
      }
      return;
    }

    // Not a gRPC request. Pass down the pipeline to Jooby's NettyHandler
    super.channelRead(ctx, msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (isGrpc && inputBridge != null) {
      inputBridge.cancel(); // Client disconnected abruptly
    }
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (isGrpc) {
      ctx.close();
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
}
