/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.internal.netty.SlowPathChecks.*;
import static io.netty.handler.codec.http.HttpUtil.isTransferEncodingChunked;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.*;
import io.jooby.netty.NettyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final Logger log = LoggerFactory.getLogger(NettyServer.class);
  private static final CharSequence server = NettyString.of("N");
  public static final CharSequence CONTENT_TYPE = NettyString.of("content-type");
  public static final CharSequence TEXT_PLAIN = NettyString.of("text/plain");
  public static final CharSequence DATE = NettyString.of("date");
  public static final CharSequence SERVER = NettyString.of("server");
  private final NettyDateService serverDate;
  private final List<Jooby> applications;
  private Router router;
  private final Context.Selector ctxSelector;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final long maxRequestSize;
  private long contentLength;
  private long chunkSize;
  private final boolean http2;
  private NettyContext context;

  public NettyHandler(
      NettyDateService dateService,
      List<Jooby> applications,
      long maxRequestSize,
      int bufferSize,
      boolean defaultHeaders,
      boolean http2) {
    this.serverDate = dateService;
    this.applications = applications;
    this.ctxSelector = Context.Selector.create(applications);
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (isHttpRequest(msg)) {
      var req = (HttpRequest) msg;
      var path = pathOnly(req.uri());
      var app = ctxSelector.select(applications, path);
      this.router = app.getRouter();

      context = new NettyContext(ctx, req, app, path, bufferSize, http2);

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

  private void release(HttpContent ref) {
    if (ref.refCnt() > 0) {
      ref.release();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    if (context != null) {
      context.flush();
    }
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
