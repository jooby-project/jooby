/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.MediaType;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.StatusCode;
import io.jooby.WebSocketCloseStatus;
import io.jooby.exception.StatusCodeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private static final AtomicReference<String> cachedDateString = new AtomicReference<>();

  private static final Runnable INVALIDATE_TASK = () -> cachedDateString.set(null);

  private static final AsciiString server = AsciiString.cached("N");
  private final ScheduledExecutorService scheduler;

  private static final int DATE_INTERVAL = 1000;

  private final Router router;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private NettyContext context;

  private final HttpDataFactory factory;
  private InterfaceHttpPostRequestDecoder decoder;

  private final long maxRequestSize;
  private long contentLength;
  private long chunkSize;

  public NettyHandler(ScheduledExecutorService scheduler, Router router, long maxRequestSize,
      int bufferSize, HttpDataFactory factory, boolean defaultHeaders) {
    this.scheduler = scheduler;
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.factory = factory;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
      if (msg instanceof HttpRequest) {
        HttpRequest req = (HttpRequest) msg;
        context = new NettyContext(ctx, req, router, pathOnly(req.uri()), bufferSize);

        if (defaultHeaders) {
          context.setHeaders.set(HttpHeaderNames.DATE, date(scheduler));
          context.setHeaders.set(HttpHeaderNames.SERVER, server);
        }
        context.setHeaders.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

        contentLength = contentLength(req);
        if (contentLength > 0 || HttpUtil.isTransferEncodingChunked(req)) {
          decoder = newDecoder(req, factory);
        } else {
          router.match(context).execute(context);
        }
      } else if (decoder != null && msg instanceof HttpContent) {
        HttpContent chunk = (HttpContent) msg;
        chunkSize += chunk.content().readableBytes();
        if (chunkSize > maxRequestSize) {
          resetDecoderState(true);
          context.sendError(new StatusCodeException(StatusCode.REQUEST_ENTITY_TOO_LARGE));
          return;
        }

        offer(chunk);

        if (contentLength == chunkSize && !(chunk instanceof LastHttpContent)) {
          chunk = LastHttpContent.EMPTY_LAST_CONTENT;
          offer(chunk);
        }

        if (chunk instanceof LastHttpContent) {
          context.decoder = decoder;
          Router.Match result = router.match(context);
          resetDecoderState(!result.matches());
          result.execute(context);
        }
      } else if (msg instanceof WebSocketFrame) {
        if (context.webSocket != null) {
          context.webSocket.handleFrame((WebSocketFrame) msg);
        }
      }
    } finally {
      if (msg instanceof ReferenceCounted) {
        ReferenceCounted ref = (ReferenceCounted) msg;
        if (ref.refCnt() > 0) {
          ref.release();
        }
      }
    }
  }

  @Override public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (context != null) {
      context.flush();
    }
  }

  @Override public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
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
            log.debug("%s %s", context.getMethod(), context.getRequestPath(), cause);
          }
        }
      } else {
        if (context == null) {
          log.error("execution resulted in exception", cause);
        } else {
          context.sendError(cause);
          context = null;
        }
      }
    } finally {
      ctx.close();
    }
  }

  private void offer(HttpContent chunk) {
    try {
      decoder.offer(chunk);
    } catch (HttpPostRequestDecoder.ErrorDataDecoderException x) {
      resetDecoderState(true);
      context.sendError(x, StatusCode.BAD_REQUEST);
    }
  }

  private void resetDecoderState(boolean destroy) {
    chunkSize = 0;
    contentLength = -1;
    if (destroy && decoder != null) {
      decoder.destroy();
    }
    decoder = null;
  }

  private static InterfaceHttpPostRequestDecoder newDecoder(HttpRequest request,
      HttpDataFactory factory) {
    String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentType != null) {
      String lowerContentType = contentType.toLowerCase();
      if (lowerContentType.startsWith(MediaType.MULTIPART_FORMDATA)) {
        return new HttpPostMultipartRequestDecoder(factory, request, StandardCharsets.UTF_8);
      } else if (lowerContentType.startsWith(MediaType.FORM_URLENCODED)) {
        return new HttpPostStandardRequestDecoder(factory, request, StandardCharsets.UTF_8);
      }
    }
    return new HttpRawPostRequestDecoder(factory.createAttribute(request, "body"));
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

  private static String date(ScheduledExecutorService scheduler) {
    String dateString = cachedDateString.get();
    if (dateString == null) {
      //set the time and register a timer to invalidate it
      //note that this is racey, it does not matter if multiple threads do this
      //the perf cost of synchronizing would be more than the perf cost of multiple threads running it
      long realTime = System.currentTimeMillis();
      long mod = realTime % DATE_INTERVAL;
      long toGo = DATE_INTERVAL - mod;
      dateString = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
      if (cachedDateString.compareAndSet(null, dateString)) {
        scheduler.schedule(INVALIDATE_TASK, toGo, TimeUnit.MILLISECONDS);
      }
    }
    return dateString;
  }
}

