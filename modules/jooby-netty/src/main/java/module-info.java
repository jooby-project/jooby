/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.Server;
import io.jooby.netty.NettyServer;

/**
 * Netty module.
 */
module io.jooby.netty {
  exports io.jooby.netty;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires io.netty.transport;
  requires io.netty.codec.http;
  requires io.netty.codec.http2;
  requires io.netty.handler;
  requires io.netty.common;
  requires io.netty.buffer;
  requires io.netty.codec;
  requires static io.netty.transport.classes.epoll;
  requires static io.netty.transport.classes.kqueue;
  requires static io.netty.incubator.transport.classes.io_uring;

  provides Server with
      NettyServer;
}
