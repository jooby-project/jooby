/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.internal.netty.SlowPathChecks.*;
import static io.netty.handler.codec.http.HttpUtil.isTransferEncodingChunked;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.WebSocketCloseStatus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AsciiString;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private static final AtomicReference<String> cachedDateString = new AtomicReference<>();

  private static final Runnable INVALIDATE_TASK = () -> cachedDateString.set(null);

  private static final AsciiString server = AsciiString.cached("N");
  private final ScheduledExecutorService scheduler;
  private static final int DATE_INTERVAL = 1000;
  private final Router router;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final HttpDataFactory factory;
  private final long maxRequestSize;
  private long contentLength;
  private long chunkSize;
  private boolean http2;
  private NettyContext context;

  public NettyHandler(
      ScheduledExecutorService scheduler,
      Router router,
      long maxRequestSize,
      int bufferSize,
      HttpDataFactory factory,
      boolean defaultHeaders,
      boolean http2) {
    this.scheduler = scheduler;
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.factory = factory;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
    this.http2 = http2;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (isHttpRequest(msg)) {
      var req = (HttpRequest) msg;

      context = new NettyContext(ctx, req, router, pathOnly(req.uri()), bufferSize, http2);

      if (defaultHeaders) {
        context.setHeaders.set(HttpHeaderNames.DATE, date(router.getLog(), scheduler));
        context.setHeaders.set(HttpHeaderNames.SERVER, server);
      }
      context.setHeaders.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

      if (req.method().equals(HttpMethod.GET)) {
        router.match(context).execute(context);
      } else {
        // possibly body:
        contentLength = contentLength(req);
        if (contentLength > 0 || isTransferEncodingChunked(req)) {
          context.decoder = newDecoder(req, factory);
        } else {
          // no body, move on
          router.match(context).execute(context);
        }
      }
    } else if (isLastHttpContent(msg)) {
      // when decoder == null, chunk is always a LastHttpContent.EMPTY, ignore it
      if (context.decoder != null) {
        offer(context, (HttpContent) msg);
        Router.Match route = router.match(context);
        resetDecoderState(context, !route.matches());
        route.execute(context);
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
      Logger log = router.getLog();
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
          if (router.isStopped()) {
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
    } catch (HttpPostRequestDecoder.ErrorDataDecoderException x) {
      resetDecoderState(context, true);
      context.sendError(x, StatusCode.BAD_REQUEST);
    }
  }

  private void resetDecoderState(NettyContext context, boolean destroy) {
    chunkSize = 0;
    contentLength = -1;
    if (destroy && context.decoder != null) {
      var decoder = context.decoder;
      context.decoder = null;
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

  private static String date(Logger log, ScheduledExecutorService scheduler) {
    var dateString = cachedDateString.get();
    if (dateString == null) {
      // set the time and register a timer to invalidate it
      // note that this is racey, it does not matter if multiple threads do this
      // the perf cost of synchronizing would be more than the perf cost of multiple threads running
      // it
      long realTime = System.currentTimeMillis();
      long mod = realTime % DATE_INTERVAL;
      long toGo = DATE_INTERVAL - mod;
      dateString = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
      if (cachedDateString.compareAndSet(null, dateString)) {
        try {
          scheduler.schedule(INVALIDATE_TASK, toGo, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException rejected) {
          if (scheduler.isShutdown()) {
            log.trace("server is shutting down", rejected);
          } else {
            throw SneakyThrows.propagate(rejected);
          }
        }
      }
    }
    return dateString;
  }
}
