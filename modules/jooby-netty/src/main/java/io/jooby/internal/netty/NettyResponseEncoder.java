/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

public class NettyResponseEncoder extends HttpResponseEncoder {
  @Override
  protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
    if (headers instanceof HeadersMultiMap headersMultiMap) {
      headersMultiMap.encode(buf);
    } else {
      super.encodeHeaders(headers, buf);
    }
  }

  @Override
  public boolean acceptOutboundMessage(Object msg) throws Exception {
    // fast-path singleton(s)
    if (msg == Unpooled.EMPTY_BUFFER || msg == LastHttpContent.EMPTY_LAST_CONTENT) {
      return true;
    }
    // JDK type checks vs non-implemented interfaces costs O(N), where
    // N is the number of interfaces already implemented by the concrete type that's being tested.
    // !(msg instanceof HttpRequest) is supposed to always be true (and meaning that msg isn't a
    // HttpRequest),
    // but sadly was part of the original behaviour of this method and cannot be removed.
    // We place here exact checks vs DefaultHttpResponse and DefaultFullHttpResponse because bad
    // users can
    // extends such types and make them to implement HttpRequest (non-sense, but still possible).
    final Class<?> msgClass = msg.getClass();
    if (msgClass == AssembledFullHttpResponse.class || msgClass == AssembledHttpResponse.class) {
      return true;
    }
    return super.acceptOutboundMessage(msg) && !(msg instanceof HttpRequest);
  }
}
