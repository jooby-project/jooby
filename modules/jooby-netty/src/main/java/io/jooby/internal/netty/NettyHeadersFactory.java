/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.HttpHeadersFactory;

public class NettyHeadersFactory implements HttpHeadersFactory {

  public static NettyHeadersFactory HEADERS = new NettyHeadersFactory();

  @Override
  public HeadersMultiMap newHeaders() {
    return new HeadersMultiMap();
  }

  @Override
  public HeadersMultiMap newEmptyHeaders() {
    return new HeadersMultiMap();
  }
}
