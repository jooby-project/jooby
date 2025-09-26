/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.internal.netty.NettyString.*;
import static io.jooby.internal.netty.SlowPathChecks.*;
import static io.netty.handler.codec.http.HttpUtil.isTransferEncodingChunked;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.netty.NettyServer;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final Logger log = LoggerFactory.getLogger(NettyServer.class);
  private final NettyDateService serverDate;
  private Router router;
  private final Context.Selector contextSelector;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final long maxRequestSize;
  private long contentLength;
  private long chunkSize;
  private final boolean http2;
  private NettyContext context;
  private boolean read;
  private boolean flush;
  private ChannelHandlerContext channelContext;

  public NettyHandler(
      NettyDateService serverDate,
      Context.Selector contextSelector,
      long maxRequestSize,
      int bufferSize,
      boolean defaultHeaders,
      boolean http2) {
    this.serverDate = serverDate;
    this.contextSelector = contextSelector;
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.channelContext = ctx;
    super.handlerAdded(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (isHttpRequest(msg)) {
      this.read = true;
      var req = (HttpRequest) msg;
      var path = pathOnly(req.uri());
      var app = contextSelector.select(path);
      this.router = app.getRouter();

      context = new NettyContext(this, ctx, req, app, path, bufferSize, http2);

      if (defaultHeaders) {
        context.setHeaders.set(DATE, serverDate.date());
        context.setHeaders.set(SERVER, server);
      }
      context.setHeaders.set(CONTENT_TYPE, TEXT_PLAIN);

      if (req.method() == HttpMethod.GET) {
        router.match(context).execute(context);
      } else {
        // possibly body:
        contentLength = contentLength(req);
        if (contentLength > 0 || isTransferEncodingChunked(req)) {
          context.httpDataFactory = new DefaultHttpDataFactory(bufferSize);
          context.httpDataFactory.setBaseDir(app.getTmpdir().toString());
          context.decoder = newDecoder(req, context.httpDataFactory);
        } else {
          // no body, move on
          router.match(context).execute(context);
        }
      }
    } else if (isLastHttpContent(msg)) {
      var chunk = (HttpContent) msg;
      try {
        // when decoder == null, chunk is always a LastHttpContent.EMPTY, ignore it
        if (context.decoder != null) {
          offer(context, chunk);
          Router.Match route = router.match(context);
          resetDecoderState(context, !route.matches());
          route.execute(context);
        }
      } finally {
        release(chunk);
      }
    } else if (isHttpContent(msg)) {
      this.read = true;
      var chunk = (HttpContent) msg;
      try {
        // when decoder == null, chunk is always a LastHttpContent.EMPTY, ignore it
        if (context.decoder != null) {
          chunkSize += chunk.content().readableBytes();
          if (chunkSize > maxRequestSize) {
            resetDecoderState(context, true);
            router.match(context).execute(context, Route.REQUEST_ENTITY_TOO_LARGE);
            return;
          }
          offer(context, chunk);
        }
      } finally {
        // must be released
        release(chunk);
      }
    } else if (isWebSocketFrame(msg)) {
      if (context.webSocket != null) {
        context.webSocket.handleFrame((WebSocketFrame) msg);
      }
    }
  }

  public void writeHttpObject(Object msg, ChannelPromise promise) {
    if (this.channelContext.executor().inEventLoop()) {
      if (this.read) {
        this.flush = true;
        this.channelContext.write(msg, promise);
      } else {
        this.channelContext.writeAndFlush(msg, promise);
      }
    } else {
      this.channelContext.executor().execute(() -> writeHttpObject(msg, promise));
    }
  }

  public void writeHttpChunk(Object header, Object body, Object last, ChannelPromise promise) {
    if (this.channelContext.executor().inEventLoop()) {
      // Headers
      channelContext.write(header, channelContext.voidPromise());
      // Body
      channelContext.write(body, channelContext.voidPromise());
      // Finish
      channelContext.writeAndFlush(last, promise);
    } else {
      this.channelContext.executor().execute(() -> writeHttpChunk(header, body, last, promise));
    }
  }

  public void writeHttpChunk(Object header, Object body, ChannelPromise promise) {
    if (this.channelContext.executor().inEventLoop()) {
      // Headers
      channelContext.write(header, channelContext.voidPromise());
      // Body + Last
      channelContext.writeAndFlush(body, promise);
    } else {
      this.channelContext.executor().execute(() -> writeHttpChunk(header, body, promise));
    }
  }

  private void release(HttpContent ref) {
    if (ref.refCnt() > 0) {
      ref.release();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (this.read) {
      this.read = false;
      if (this.flush) {
        this.flush = false;
        ctx.flush();
      }
    }
    if (context != null) {
      //      context.destroy(null);
    }
    super.channelReadComplete(ctx);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
      NettyWebSocket ws = ctx.channel().attr(NettyWebSocket.WS).getAndSet(null);
      if (ws != null) {
        ws.close(WebSocketCloseStatus.GOING_AWAY);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    try {
      if (Server.connectionLost(cause)) {
        if (log.isDebugEnabled()) {
          if (context == null) {
            log.debug("execution resulted in connection lost", cause);
          } else {
            log.debug("{} {}", context.getMethod(), context.getRequestPath(), cause);
          }
        }
      } else {
        if (context == null) {
          log.error("execution resulted in exception", cause);
        } else {
          if (context.getRouter().isStopped()) {
            log.debug("execution resulted in exception while application was shutting down", cause);
          } else {
            context.sendError(cause);
          }
        }
      }
    } finally {
      ctx.close();
    }
  }

  private void offer(NettyContext context, HttpContent chunk) {
    try {
      context.decoder.offer(chunk);
    } catch (HttpPostRequestDecoder.ErrorDataDecoderException
        | HttpPostRequestDecoder.TooLongFormFieldException
        | HttpPostRequestDecoder.TooManyFormFieldsException x) {
      resetDecoderState(context, true);
      context.sendError(x, StatusCode.BAD_REQUEST);
    }
  }

  private void resetDecoderState(NettyContext context, boolean destroy) {
    chunkSize = 0;
    contentLength = -1;
    if (destroy && context.decoder != null) {
      var decoder = context.decoder;
      var httpDataFactory = context.httpDataFactory;
      context.decoder = null;
      context.httpDataFactory = null;
      httpDataFactory.cleanAllHttpData();
      decoder.destroy();
    }
  }

  private static InterfaceHttpPostRequestDecoder newDecoder(
      HttpRequest request, HttpDataFactory factory) {
    String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentType != null) {
      String lowerContentType = contentType.toLowerCase();
      if (lowerContentType.startsWith(MediaType.MULTIPART_FORMDATA)) {
        return new HttpPostMultipartRequestDecoder(factory, request, StandardCharsets.UTF_8);
      } else if (lowerContentType.startsWith(MediaType.FORM_URLENCODED)) {
        return new HttpPostStandardRequestDecoder(factory, request, StandardCharsets.UTF_8);
      }
    }
    return new HttpRawPostRequestDecoder(factory, request);
  }

  static String pathOnly(String uri) {
    int len = uri.indexOf('?');
    return len > 0 ? uri.substring(0, len) : uri;
  }

  private static long contentLength(HttpRequest req) {
    String value = req.headers().get(HttpHeaderNames.CONTENT_LENGTH);
    if (value == null) {
      return -1;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException x) {
      return -1;
    }
  }
}
