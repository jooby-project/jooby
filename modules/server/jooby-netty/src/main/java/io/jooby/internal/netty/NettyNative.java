/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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
