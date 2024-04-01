/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.LastHttpContent;

public class NettyResponseEncoder extends HttpResponseEncoder {
  @Override
  protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
    if (headers instanceof HeadersMultiMap) {
      HeadersMultiMap vertxHeaders = (HeadersMultiMap) headers;
      vertxHeaders.encode(buf);
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
    return super.acceptOutboundMessage(msg);
  }
}
