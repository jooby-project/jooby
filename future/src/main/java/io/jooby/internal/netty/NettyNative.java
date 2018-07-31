package io.jooby.internal.netty;

import io.jooby.Server;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ThreadPerTaskExecutor;

public interface NettyNative {

  EventLoopGroup group(int threads);

  Class<? extends ServerSocketChannel> channel();

  static NettyNative get() {
    /** Epoll: */
    if (Epoll.isAvailable()) {
      return new NettyNative() {
        @Override public EventLoopGroup group(int threads) {
          return new EpollEventLoopGroup(threads);
        }

        @Override public Class<? extends ServerSocketChannel> channel() {
          return EpollServerSocketChannel.class;
        }
      };
    }
    // Found slower than IO:
    //    if (KQueue.isAvailable()) {
    //      return new NettyNative() {
    //        @Override public EventLoopGroup group(int threads) {
    //          return new KQueueEventLoopGroup(threads);
    //        }
    //
    //        @Override public Class<? extends ServerSocketChannel> channel() {
    //          return KQueueServerSocketChannel.class;
    //        }
    //      };
    //    }
    return new NettyNative() {
      @Override public EventLoopGroup group(int threads) {
        return new NioEventLoopGroup(threads);
      }

      @Override public Class<? extends ServerSocketChannel> channel() {
        return NioServerSocketChannel.class;
      }
    };
  }
}
