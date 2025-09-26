/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.vertx;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

public class EventLoopGroupProxy extends MultithreadEventLoopGroup {

  /**
   * Creates a event loop group with the specified amount of threads provided by the specified root
   * event loop group.
   *
   * @param nThreads the number of threads to allocate to the group
   * @param rootEventLoopGroup the root event loop group
   */
  public EventLoopGroupProxy(int nThreads, EventLoopGroup rootEventLoopGroup) {
    super(nThreads, (Executor) null, rootEventLoopGroup);
  }

  @Override
  protected EventLoop newChild(Executor executor, Object... args) {
    return ((EventLoopGroup) args[0]).next();
  }

  /** Event loop group is shutdown when the coreEventLoopGroup is shutdown. */
  @Override
  @Deprecated
  public void shutdown() {}

  /** Event loop group is shutdown when the coreEventLoopGroup is shutdown. */
  @Override
  @Deprecated
  public List<Runnable> shutdownNow() {
    return List.of();
  }

  /** Event loop group is shutdown when the coreEventLoopGroup is shutdown. */
  @Override
  public Future<?> shutdownGracefully() {
    return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
  }

  /** Event loop group is shutdown when the coreEventLoopGroup is shutdown. */
  @Override
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
    return new DefaultPromise<>(GlobalEventExecutor.INSTANCE).setSuccess(null);
  }
}
