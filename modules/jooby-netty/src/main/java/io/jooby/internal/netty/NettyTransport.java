/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringChannelOption;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
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
    if (isIOUring(loader)) {
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
    return new IOUringTransport();
  }

  private static boolean isIOUring(ClassLoader loader) {
    try {
      loader.loadClass("io.netty.incubator.channel.uring.IOUring");
      return IOUring.isAvailable();
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
      NioEventLoopGroup loopGroup =
          new NioEventLoopGroup(threads, new DefaultThreadFactory(threadName));
      loopGroup.setIoRatio(ioRatio);
      return loopGroup;
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop).channel(NioServerSocketChannel.class);
    }
  }

  private static class IOUringTransport extends NettyTransport {

    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      IOUringEventLoopGroup loopGroup =
          new IOUringEventLoopGroup(threads, new DefaultThreadFactory(threadName + "-io-uring"));
      return loopGroup;
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop)
          .channel(IOUringServerSocketChannel.class)
          .option(IOUringChannelOption.SO_REUSEPORT, true);
    }
  }

  private static class EpollTransport extends NettyTransport {
    @Override
    public EventLoopGroup createEventLoop(int threads, String threadName, int ioRatio) {
      EpollEventLoopGroup loopGroup =
          new EpollEventLoopGroup(threads, new DefaultThreadFactory(threadName + "-epoll"));
      return loopGroup;
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
      KQueueEventLoopGroup loopGroup =
          new KQueueEventLoopGroup(threads, new DefaultThreadFactory(threadName + "-kqueue"));
      loopGroup.setIoRatio(ioRatio);
      return loopGroup;
    }

    @Override
    public ServerBootstrap configure(EventLoopGroup acceptor, EventLoopGroup eventloop) {
      return super.configure(acceptor, eventloop).channel(KQueueServerSocketChannel.class);
    }
  }
}
