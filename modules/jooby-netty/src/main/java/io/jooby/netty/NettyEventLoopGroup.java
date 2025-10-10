/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import java.util.concurrent.ExecutorService;

import io.netty.channel.EventLoopGroup;

/** Allow to customize netty event loop group and worker executor. */
public interface NettyEventLoopGroup {
  /**
   * Acceptor or parent event loop group.
   *
   * @return Acceptor or parent event loop group.
   */
  EventLoopGroup acceptor();

  /**
   * IO or child event loop group.
   *
   * @return IO or child event loop group.
   */
  EventLoopGroup eventLoop();

  /**
   * Worker executor.
   *
   * @return Worker executor.
   */
  ExecutorService worker();

  void shutdown();
}
