/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.StatusCodeException;
import io.jooby.MediaType;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.StatusCode;
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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
    }
  };

  private static final AsciiString server = AsciiString.cached("N");

  private volatile AsciiString date = new AsciiString(FORMAT.get().format(new Date()));

  private static final int DATE_INTERVAL = 1000;

  private final Router router;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private NettyContext context;
  private Router.Match result;

  private final HttpDataFactory factory;
  private InterfaceHttpPostRequestDecoder decoder;

  private final long maxRequestSize;
  private long contentLength;
  private long chunkSize;

  public NettyHandler(ScheduledExecutorService scheduler, Router router, long maxRequestSize,
      int bufferSize, HttpDataFactory factory, boolean defaultHeaders) {
    scheduler
        .scheduleWithFixedDelay(dateSync(FORMAT.get()), DATE_INTERVAL, DATE_INTERVAL, MILLISECONDS);
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.factory = factory;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
  }

  private Runnable dateSync(DateFormat format) {
    return () -> date = new AsciiString(format.format(new Date()));
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      context = new NettyContext(ctx, req, router, pathOnly(req.uri()), bufferSize);

      if (defaultHeaders) {
        context.setHeaders.set(HttpHeaderNames.DATE, date);
        context.setHeaders.set(HttpHeaderNames.SERVER, server);
      }
      context.setHeaders.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

      result = router.match(context);

      contentLength = contentLength(req);
      if (contentLength > 0 || HttpUtil.isTransferEncodingChunked(req)) {
        decoder = newDecoder(req, factory);
      } else {
        result.execute(context);
      }
    } else if (decoder != null && msg instanceof HttpContent) {
      HttpContent chunk = (HttpContent) msg;
      chunkSize += chunk.content().readableBytes();
      if (chunkSize > maxRequestSize) {
        resetDecoderState(true);
        chunk.release();
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
        resetDecoderState(false);
        result.execute(context);
      }
    } else if (msg instanceof WebSocketFrame) {
      if (context.webSocket != null) {
        context.webSocket.handleFrame((WebSocketFrame) msg);
      }
    }
  }

  @Override public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (context != null) {
      context.flush();
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
            log.debug("%s %s", context.getMethod(), context.pathString(), cause);
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
}

