/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public interface NettyNative {

  EventLoopGroup group(String name, int threads);

  Class<? extends ServerSocketChannel> channel();

  static NettyNative get(ClassLoader loader) {
    try {
      loader.loadClass("io.netty.channel.epoll.Epoll");
      return epollOrNio();
    } catch (ClassNotFoundException x) {
      return nio();
    }
  }

  static NettyNative epollOrNio() {
    if (Epoll.isAvailable()) {
      return new NettyNative() {
        @Override public EventLoopGroup group(String name, int threads) {
          return new EpollEventLoopGroup(threads, new DefaultThreadFactory(name + "-epoll"));
        }

        @Override public Class<? extends ServerSocketChannel> channel() {
          return EpollServerSocketChannel.class;
        }
      };
    }
    return nio();
  }

  static NettyNative nio() {
    return new NettyNative() {
      @Override public EventLoopGroup group(String name, int threads) {
        return new NioEventLoopGroup(threads, new DefaultThreadFactory(name + "-nio"));
      }

      @Override public Class<? extends ServerSocketChannel> channel() {
        return NioServerSocketChannel.class;
      }
    };
  }
}
