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
package io.jooby.netty;

import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.internal.netty.DefaultHeaders;
import io.jooby.internal.netty.NettyNative;
import io.jooby.internal.netty.NettyPipeline;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Netty extends Server.Base {

  private List<Jooby> applications = new ArrayList<>();

  private EventLoopGroup acceptor;

  private EventLoopGroup ioLoop;

  private int port = 8080;

  private boolean gzip;

  private long maxRequestSize = _10MB;

  private DefaultEventExecutorGroup worker;

  private int bufferSize = _16KB;

  private int processors = Math.max(Runtime.getRuntime().availableProcessors(), 1);

  private int acceptorThreads = processors;

  private int ioThreads = processors + (processors / 2);

  private int workerThreads = processors * 8;

  private boolean defaultHeaders = true;

  private ResourceLeakDetector.Level leakDectectorLevel;

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Nonnull @Override public Server defaultHeaders(boolean value) {
    defaultHeaders = value;
    return this;
  }

  @Nonnull @Override public Server maxRequestSize(long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    return this;
  }

  @Nonnull @Override public Server bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public Server gzip(boolean enabled) {
    this.gzip = enabled;
    return this;
  }

  public Server leakDectectorLevel(@Nonnull ResourceLeakDetector.Level level) {
    this.leakDectectorLevel = level;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Override public Server workerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
    return this;
  }

  @Nonnull @Override public Server start(Jooby application) {
    try {

      if (leakDectectorLevel == null) {
        leakDectectorLevel = ResourceLeakDetector.Level
            .valueOf(System.getProperty("io.netty.leakDetection.level", "disabled").toUpperCase());
      }
      ResourceLeakDetector.setLevel(leakDectectorLevel);

      applications.add(application);

      addShutdownHook();

      /** Worker: Application blocking code */
      worker = new DefaultEventExecutorGroup(workerThreads,
          new DefaultThreadFactory("netty-worker"));
      fireStart(applications, worker);

      /** Disk attributes: */
      if (applications.size() == 1) {
        String tmpdir = applications.get(0).tmpdir().toString();
        DiskFileUpload.baseDirectory = tmpdir;
        DiskAttribute.baseDirectory = tmpdir;
      } else {
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.baseDirectory = null; // system temp directory
      }

      NettyNative provider = NettyNative.get(getClass().getClassLoader());
      /** Acceptor: Accepts connections */
      this.acceptor = provider.group("netty-acceptor", acceptorThreads);

      /** IO: processing connections, parsing messages and doing engine's internal work */
      this.ioLoop = provider.group("netty", ioThreads);

      /** File data factory: */
      HttpDataFactory factory = new DefaultHttpDataFactory(bufferSize);

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 8192);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);

      Consumer<HttpHeaders> defaultHeaders = this.defaultHeaders
          ? defaultHeaders(acceptor.next())
          : defaultResponseType();

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .childHandler(
              new NettyPipeline(applications.get(0), factory, defaultHeaders, gzip, bufferSize,
                  maxRequestSize))
          .childOption(ChannelOption.SO_REUSEADDR, true)
          .childOption(ChannelOption.TCP_NODELAY, true);

      bootstrap.bind(port).sync();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }

    fireReady(applications);

    return this;
  }

  public Server stop() {
    fireStop(applications);
    applications = null;
    if (ioLoop != null) {
      ioLoop.shutdownGracefully();
      ioLoop = null;
    }
    if (acceptor != null) {
      acceptor.shutdownGracefully();
      acceptor = null;
    }
    if (worker != null) {
      worker.shutdownGracefully();
      worker = null;
    }
    return this;
  }

  private static DefaultHeaders defaultHeaders(ScheduledExecutorService executor) {
    DefaultHeaders headers = new DefaultHeaders();
    executor.scheduleWithFixedDelay(headers, 1000, 1000, TimeUnit.MILLISECONDS);
    return headers;
  }

  private static Consumer<HttpHeaders> defaultResponseType() {
    return headers -> headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
  }
}
