/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class NettyResponseEncoder extends HttpResponseEncoder {
  @Override
  protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
    if (headers instanceof HeadersMultiMap headersMultiMap) {
      headersMultiMap.encode(buf);
    } else {
      super.encodeHeaders(headers, buf);
    }
  }
}
