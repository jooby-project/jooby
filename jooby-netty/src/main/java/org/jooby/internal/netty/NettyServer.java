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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Map.Entry;
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
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyServer implements Server {

  static {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Server.class);

  private EventLoopGroup bossLoop;

  private EventLoopGroup workerLoop;

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
    bossLoop = eventLoop(parentThreads, "parent");
    if (config.hasPath("netty.threads.Child")) {
      int childThreads = config.getInt("netty.threads.Child");
      workerLoop = eventLoop(childThreads, "child");
    } else {
      workerLoop = bossLoop;
    }

    ThreadFactory threadFactory = new DefaultThreadFactory(config.getString("netty.threads.Name"));
    DefaultEventExecutorGroup executor = new DefaultEventExecutorGroup(
        config.getInt("netty.threads.Max"), threadFactory);

    this.ch = bootstrap(executor, null, config.getInt("application.port"));

    if (config.hasPath("application.securePort")) {
      bootstrap(executor, sslCtx(config), config.getInt("application.securePort"));
    }
  }

  private Channel bootstrap(final EventExecutorGroup executor, final SslContext sslCtx,
      final int port) throws InterruptedException {
    ServerBootstrap bootstrap = new ServerBootstrap();

    bootstrap.group(bossLoop, workerLoop)
        .channel(bossLoop instanceof EpollEventLoopGroup
            ? EpollServerSocketChannel.class
            : NioServerSocketChannel.class)
        .handler(new LoggingHandler(Server.class, LogLevel.DEBUG))
        .childHandler(new NettyInitializer(executor, dispatcher, config, sslCtx));

    configure(config.getConfig("netty.options"), "netty.options",
        (option, value) -> bootstrap.option(option, value));

    configure(config.getConfig("netty.child.options"), "netty.child.options",
        (option, value) -> bootstrap.childOption(option, value));

    return bootstrap
        .bind(host(config.getString("application.host")), port)
        .sync()
        .channel();
  }

  private String host(final String host) {
    return "localhost".equals(host) ? "0.0.0.0" : host;
  }

  @Override
  public void stop() throws Exception {
    bossLoop.shutdownGracefully();
    if (!workerLoop.isShutdown()) {
      workerLoop.shutdownGracefully();
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

  private EventLoopGroup eventLoop(final int threads, final String name) {
    log.debug("netty.threads.{}({})", name, threads);
    DefaultThreadFactory threadFactory = new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup(threads, threadFactory);
    }
    return new NioEventLoopGroup(threads, threadFactory);
  }

  private SslContext sslCtx(final Config config) throws IOException, CertificateException {
    String tmpdir = config.getString("application.tmpdir");
    File keyStoreCert = toFile(config.getString("ssl.keystore.cert"), tmpdir);
    File keyStoreKey = toFile(config.getString("ssl.keystore.key"), tmpdir);
    String keyStorePass = config.hasPath("ssl.keystore.password")
        ? config.getString("ssl.keystore.password") : null;
    SslContextBuilder scb = SslContextBuilder.forServer(keyStoreCert, keyStoreKey, keyStorePass);
    if (config.hasPath("ssl.trust.cert")) {
      scb.trustManager(toFile(config.getString("ssl.trust.cert"), tmpdir));
    }
    return scb.build();
  }

  static File toFile(final String path, final String tmpdir) throws IOException {
    File file = new File(path);
    if (file.exists()) {
      return file;
    }
    file = new File(tmpdir, Paths.get(path).getFileName().toString());
    // classpath resource?
    try (InputStream in = NettyServer.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new FileNotFoundException(path);
      }
      Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    file.deleteOnExit();
    return file;
  }

}
