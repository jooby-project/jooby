/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;

public class NettyEventLoopGroup {
  private final EventLoopGroup parent;
  private final EventLoopGroup child;
  private boolean closed;

  public NettyEventLoopGroup(NettyTransport transport, boolean single, int ioThreads) {
    child = transport.createEventLoop(ioThreads, "eventloop", 100);
    if (single) {
      parent = child;
    } else {
      parent = transport.createEventLoop(1, "acceptor", 50);
    }
  }

  public EventLoopGroup getParent() {
    return parent;
  }

  public EventLoopGroup getChild() {
    return child;
  }

  public void shutdown() {
    if (!closed) {
      closed = true;
      if (parent == child) {
        shutdown(child, 0);
      } else {
        try {
          shutdown(parent, 0);
        } finally {
          shutdown(child, 2);
        }
      }
    }
  }

  private void shutdown(EventLoopGroup eventLoopGroup, int quietPeriod) {
    eventLoopGroup.shutdownGracefully(quietPeriod, 15, TimeUnit.SECONDS);
  }
}
