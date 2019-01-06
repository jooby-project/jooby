/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.Err;
import io.jooby.MediaType;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.StatusCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

import java.nio.charset.StandardCharsets;

public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final Router router;
  private final long maxRequestSize;
  private final HttpDataFactory factory;
  private InterfaceHttpPostRequestDecoder decoder;
  private NettyContext context;
  private long contentLength;
  private long chunkSize;

  public NettyHandler(Router router, long maxRequestSize, HttpDataFactory factory) {
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.factory = factory;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      context = new NettyContext(ctx, req, router, pathOnly(req.uri()));
      Router.Match result = router.match(context);
      /** Don't check/parse for body if there is no match: */
      if (result.matches()) {
        contentLength = HttpUtil.getContentLength(req, -1L);
        boolean chunked = HttpUtil.isTransferEncodingChunked(req);
        if (contentLength > 0 || chunked) {
          decoder = newDecoder(req, factory);
        } else {
          result.execute(context);
        }
      } else {
        result.execute(context);
      }
    } else if (decoder != null && msg instanceof HttpContent) {
      HttpContent chunk = (HttpContent) msg;
      chunkSize += chunk.content().readableBytes();
      if (chunkSize > maxRequestSize) {
        resetDecoderState(true);
        chunk.release();
        context.sendError(new Err(StatusCode.REQUEST_ENTITY_TOO_LARGE));
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
        router.match(context).execute(context);
      }
    } else {
      ctx.fireChannelRead(msg);
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

  private InterfaceHttpPostRequestDecoder newDecoder(HttpRequest request, HttpDataFactory factory) {
    String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    if (contentType != null) {
      String lowerContentType = contentType.toLowerCase();
      if (lowerContentType.startsWith(MediaType.MULTIPART_FORMDATA)) {
        return new HttpPostMultipartRequestDecoder(factory, request, StandardCharsets.UTF_8);
      } else if (lowerContentType.equals(MediaType.FORM_URLENCODED)) {
        return new HttpPostStandardRequestDecoder(factory, request, StandardCharsets.UTF_8);
      }
    }
    return new HttpRawPostRequestDecoder(factory.createAttribute(request, "body"));
  }

  static String pathOnly(String uri) {
    int len = uri.length();
    for (int i = 0; i < len; i++) {
      char c = uri.charAt(i);
      if (c == '?') {
        return uri.substring(0, i);
      }
    }
    return uri;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    try {
      if (Server.connectionLost(cause)) {

      } else {
        if (context == null) {
          // log
        } else {
          context.sendError(cause);
          context = null;
        }
      }
    } finally {
      ctx.close();
    }
  }
}

