/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.rpc.grpc.GrpcProcessor;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

public class NettyGrpcHandler extends ChannelInboundHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(NettyGrpcHandler.class);

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
      var contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
      var path = req.uri();
      int queryIndex = path.indexOf('?');
      path = queryIndex > 0 ? path.substring(0, queryIndex) : path;

      if (processor.isGrpcMethod(path)
          && contentType != null
          && contentType.startsWith("application/grpc")) {

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

        // This prevents leaking state on rejected HTTP/1.1 connections.
        isGrpc = true;

        // We will implement NettyGrpcExchange in the next step
        var exchange = new NettyGrpcExchange(ctx, req);
        var subscriber = processor.process(exchange);

        inputBridge = new NettyGrpcInputBridge(ctx, subscriber);
        inputBridge.start();

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
    log.debug("gRPC stream exception caught", cause);
    if (isGrpc) {
      ctx.close();
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
}
