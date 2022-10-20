/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.Http2Configurer;
import io.jooby.internal.netty.http2.NettyHttp2Configurer;

module io.jooby.http2.netty {
  requires io.jooby;
  requires io.jooby.netty;
  requires io.netty.buffer;
  requires io.netty.transport;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.codec.http2;
  requires io.netty.handler;

  provides Http2Configurer with
      NettyHttp2Configurer;
}
