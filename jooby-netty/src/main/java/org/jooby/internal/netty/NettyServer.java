/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;

public class NettyServer implements Server {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Server.class);

  private NioEventLoopGroup parentGroup;

  private NioEventLoopGroup childGroup;

  private Channel ch;

  private Config config;

  private HttpHandler dispatcher;

  @Inject
  public NettyServer(final HttpHandler dispatcher, final Config config) {
    this.dispatcher = dispatcher;
    this.config = config;
  }

  @Override
  public void start() throws Exception {
    int parentThreads = config.getInt("netty.threads.Parent");
    parentGroup = eventLoop(parentThreads, "parent");
    if (config.hasPath("netty.threads.Child")) {
      int childThreads = config.getInt("netty.threads.Child");
      childGroup = eventLoop(childThreads, "child");
    } else {
      childGroup = parentGroup;
    }

    ServerBootstrap bootstrap = new ServerBootstrap();

    DefaultEventExecutorGroup executor =
        new DefaultEventExecutorGroup(config.getInt("netty.threads.Max"),
            new DefaultThreadFactory(config.getString("netty.threads.Name")));

    bootstrap.group(parentGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(Server.class, LogLevel.DEBUG))
        .childHandler(new NettyInitializer(executor, dispatcher, config));

    configure(config.getConfig("netty.options"), "netty.options", (option, value) ->
        bootstrap.option(option, value));

    configure(config.getConfig("netty.child.options"), "netty.child.options", (option, value) ->
        bootstrap.childOption(option, value));

    this.ch = bootstrap
        .bind(config.getString("application.host"), config.getInt("application.port")).sync()
        .channel();
  }

  @Override
  public void stop() throws Exception {
    parentGroup.shutdownGracefully();
    if (!childGroup.isShutdown()) {
      childGroup.shutdownGracefully();
    }
  }

  @Override
  public void join() throws InterruptedException {
    ch.closeFuture().sync();
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private void configure(final Config config, final String path,
      final BiConsumer<ChannelOption<Object>, Object> setter) {
    config.entrySet().forEach(entry -> {
      Entry<ChannelOption, Class<?>> result = findOption(entry.getKey());
      if (result != null) {
        ChannelOption option = result.getKey();
        String optionName = entry.getKey();
        Class<?> optionType = result.getValue();
        Object value = config.getAnyRef(optionName);
        if (Number.class.isAssignableFrom(optionType)) {
          if (optionType == Integer.class) {
            value = ((Number) value).intValue();
          }
        }
        log.debug("{}.{}({})", path, option, value);
        setter.accept(option, value);
      } else {
        log.error("Unknown option: netty.channel.{}", entry.getKey());
      }
    });

  }

  @SuppressWarnings("rawtypes")
  private Map.Entry<ChannelOption, Class<?>> findOption(final String optionName) {
    try {
      Field field = ChannelOption.class.getDeclaredField(optionName);
      ChannelOption option = (ChannelOption) field.get(null);
      Class optionType = (Class) ((ParameterizedType) field.getGenericType())
          .getActualTypeArguments()[0];
      return Maps.immutableEntry(option, optionType);
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
      return null;
    }
  }

  private NioEventLoopGroup eventLoop(final int threads, final String name) {
    log.debug("netty.threads.{}({})", name, threads);
    NioEventLoopGroup group = new NioEventLoopGroup(threads,
        new DefaultThreadFactory(name, Thread.MAX_PRIORITY));
    return group;
  }
}
