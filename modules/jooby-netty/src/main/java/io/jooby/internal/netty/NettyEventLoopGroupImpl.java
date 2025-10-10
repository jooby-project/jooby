/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.netty.NettyEventLoopGroup;
import io.netty.channel.EventLoopGroup;

public class NettyEventLoopGroupImpl implements NettyEventLoopGroup {
  private final EventLoopGroup parent;
  private final EventLoopGroup child;
  private boolean closed;
  private ExecutorService worker;

  public NettyEventLoopGroupImpl(
      NettyTransport transport, boolean single, int ioThreads, ExecutorService worker) {
    child = transport.createEventLoop(ioThreads, "eventloop", 100);
    if (single) {
      parent = child;
    } else {
      parent = transport.createEventLoop(1, "acceptor", 50);
    }
    this.worker = worker;
  }

  @Override
  public @NonNull EventLoopGroup acceptor() {
    return parent;
  }

  @Override
  public @NonNull EventLoopGroup eventLoop() {
    return child;
  }

  @Override
  public @NonNull ExecutorService worker() {
    return worker;
  }

  public void shutdown() {
    if (!closed) {
      closed = true;
      try {
        if (parent == child) {
          shutdown(child, 0);
        } else {
          try {
            shutdown(parent, 0);
          } finally {
            shutdown(child, 2);
          }
        }
      } finally {
        worker.shutdown();
      }
    }
  }

  private void shutdown(EventLoopGroup eventLoopGroup, int quietPeriod) {
    eventLoopGroup.shutdownGracefully(quietPeriod, 15, TimeUnit.SECONDS);
  }
}
