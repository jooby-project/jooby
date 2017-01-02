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

import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.is;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import javaslang.control.Try;

public class NettyServer implements Server {

  static {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Server.class);

  private EventLoopGroup bossLoop;

  private EventLoopGroup workerLoop;

  private Channel ch;

  private Config conf;

  private HttpHandler dispatcher;

  private DefaultEventExecutorGroup executor;

  @Inject
  public NettyServer(final HttpHandler dispatcher, final Config config) {
    this.dispatcher = dispatcher;
    this.conf = config;
  }

  @Override
  public void start() throws Exception {
    int bossThreads = conf.getInt("netty.threads.Boss");
    bossLoop = eventLoop(bossThreads, "boss");
    int workerThreads = conf.getInt("netty.threads.Worker");
    if (workerThreads > 0) {
      workerLoop = eventLoop(workerThreads, "worker");
    } else {
      workerLoop = bossLoop;
    }

    ThreadFactory threadFactory = new DefaultThreadFactory(conf.getString("netty.threads.Name"));
    this.executor = new DefaultEventExecutorGroup(
        conf.getInt("netty.threads.Max"), threadFactory);

    this.ch = bootstrap(executor, null, conf.getInt("application.port"));

    boolean securePort = conf.hasPath("application.securePort");

    if (securePort) {
      bootstrap(executor, NettySslContext.build(conf), conf.getInt("application.securePort"));
    }
  }

  private Channel bootstrap(final EventExecutorGroup executor, final SslContext sslCtx,
      final int port) throws InterruptedException {
    ServerBootstrap bootstrap = new ServerBootstrap();

    boolean epoll = bossLoop instanceof EpollEventLoopGroup;
    bootstrap.group(bossLoop, workerLoop)
        .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
        .handler(new LoggingHandler(Server.class, LogLevel.DEBUG))
        .childHandler(new NettyPipeline(executor, dispatcher, conf, sslCtx));

    configure(conf.getConfig("netty.options"), "netty.options",
        (option, value) -> bootstrap.option(option, value));

    configure(conf.getConfig("netty.worker.options"), "netty.worker.options",
        (option, value) -> bootstrap.childOption(option, value));

    return bootstrap
        .bind(host(conf.getString("application.host")), port)
        .sync()
        .channel();
  }

  private String host(final String host) {
    return "localhost".equals(host) ? "0.0.0.0" : host;
  }

  @Override
  public void stop() throws Exception {
    Try.run(() -> executor.shutdownGracefully())
        .onFailure(x -> log.debug("executor shutdown resulted in exception", x));
    Try.run(() -> bossLoop.shutdownGracefully())
        .onFailure(x -> log.debug("event loop shutdown resulted in exception", x));
    if (!workerLoop.isShutdown()) {
      Try.run(() -> workerLoop.shutdownGracefully())
          .onFailure(x -> log.debug("worker event loop shutdown resulted in exception", x));
    }
  }

  @Override
  public void join() throws InterruptedException {
    ch.closeFuture().sync();
  }

  @Override
  public Optional<Executor> executor()
  {
    return Optional.ofNullable(executor);
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
        Object value = Match(optionType).of(
            Case(is(Boolean.class), () -> config.getBoolean(optionName)),
            Case(is(Integer.class), () -> config.getInt(optionName)),
            Case(is(Long.class), () -> config.getLong(optionName)));
        log.debug("{}.{}({})", path, option, value);
        setter.accept(option, value);
      } else {
        log.error("Unknown option: {}.{}", path, entry.getKey());
      }
    });

  }

  @SuppressWarnings("rawtypes")
  private Map.Entry<ChannelOption, Class<?>> findOption(final String optionName) {
    try {
      Field field = EpollChannelOption.class.getField(optionName);
      ChannelOption option = (ChannelOption) field.get(null);
      Class optionType = (Class) ((ParameterizedType) field.getGenericType())
          .getActualTypeArguments()[0];
      return Maps.immutableEntry(option, optionType);
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
      return null;
    }
  }

  private EventLoopGroup eventLoop(final int threads, final String name) {
    log.debug("netty.threads.{}({})", name, threads);
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup(threads, new DefaultThreadFactory("epoll-" + name, false));
    }
    return new NioEventLoopGroup(threads, new DefaultThreadFactory("nio-" + name, false));
  }

}
