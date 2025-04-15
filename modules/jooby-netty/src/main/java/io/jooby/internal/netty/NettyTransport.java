/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueChannelOption;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringChannelOption;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public abstract class NettyTransport {
  private static final int BACKLOG = 8192;

  public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.option(ChannelOption.SO_BACKLOG, BACKLOG);
    bootstrap.option(ChannelOption.SO_REUSEADDR, true);
    return bootstrap.group(acceptor, eventloop);
  }

  public abstract EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio);

  public static NettyTransport transport(ClassLoader loader) {
    if (isIoUring(loader)) {
      return ioUring();
    } else if (isEpoll(loader)) {
      return epoll();
    } else if (isKQueue(loader)) {
      return kqueue();
    }
    return nio();
  }

  private static NettyTransport kqueue() {
    return new KQueueTransport();
  }

  private static NettyTransport epoll() {
    return new EpollTransport();
  }

  private static NettyTransport ioUring() {
    return new IoUringTransport();
  }

  private static boolean isIoUring(ClassLoader loader) {
    try {
      loader.loadClass("io.netty.channel.uring.IoUring");
      return IoUring.isAvailable();
    } catch (ClassNotFoundException x) {
      return false;
    }
  }

  private static boolean isEpoll(ClassLoader loader) {
    try {
      loader.loadClass("io.netty.channel.epoll.Epoll");
      return Epoll.isAvailable();
    } catch (ClassNotFoundException x) {
      return false;
    }
  }

  private static boolean isKQueue(ClassLoader loader) {
    try {
      loader.loadClass("io.netty.channel.kqueue.KQueue");
      return KQueue.isAvailable();
    } catch (ClassNotFoundException x) {
      return false;
    }
  }

  private static NettyTransport nio() {
    return new JDKTransport();
  }

  private static class JDKTransport extends NettyTransport {
    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      return new MultiThreadIoEventLoopGroup(
          threads, new DefaultThreadFactory(threadName), NioIoHandler.newFactory());
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop).channel(NioServerSocketChannel.class);
    }
  }

  private static class IoUringTransport extends NettyTransport {

    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      return new MultiThreadIoEventLoopGroup(
          threads,
          new DefaultThreadFactory(threadName + "-io-uring"),
          IoUringIoHandler.newFactory());
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop)
          .channel(IoUringServerSocketChannel.class)
          .option(IoUringChannelOption.SO_REUSEPORT, true);
    }
  }

  private static class EpollTransport extends NettyTransport {
    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      return new MultiThreadIoEventLoopGroup(
          threads, new DefaultThreadFactory(threadName + "-epoll"), EpollIoHandler.newFactory());
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop)
          .channel(EpollServerSocketChannel.class)
          .option(EpollChannelOption.SO_REUSEPORT, true);
    }
  }

  private static class KQueueTransport extends NettyTransport {
    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      return new MultiThreadIoEventLoopGroup(
          threads, new DefaultThreadFactory(threadName + "-kqueue"), KQueueIoHandler.newFactory());
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop)
          .channel(KQueueServerSocketChannel.class)
          .option(KQueueChannelOption.SO_REUSEPORT, true);
    }
  }
}
