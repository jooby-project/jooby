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
import io.jooby.internal.netty.NettyMultiHandler;
import io.jooby.internal.netty.NettyNative;
import io.jooby.internal.netty.NettyHandler;
import io.jooby.internal.netty.NettyPipeline;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.jooby.Throwing;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Netty extends Server.Base {

  private List<Jooby> applications = new ArrayList<>();

  private EventLoopGroup acceptor;

  private EventLoopGroup ioLoop;

  private int port = 8080;

  private boolean gzip;

  private long maxRequestSize = _10MB;

  private DefaultEventExecutorGroup worker;

  private int bufferSize = _16KB;

  @Override public Server port(int port) {
    this.port = port;
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

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public Server start(Jooby application) {
    try {
      applications.add(application);

      addShutdownHook();

      int threads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
      int workerThreads = threads * 8;
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
      /** Acceptor: */
      this.acceptor = provider.group("netty-acceptor", 1);

      /** Client: */
      this.ioLoop = provider.group("netty", threads);

      /** Handler: */
      HttpDataFactory factory = new DefaultHttpDataFactory(bufferSize);

      Supplier<ChannelInboundHandler> handler = () -> new NettyHandler(applications.get(0),
          maxRequestSize, bufferSize, factory);

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 8192);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .childHandler(new NettyPipeline(handler, gzip, bufferSize))
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

}
