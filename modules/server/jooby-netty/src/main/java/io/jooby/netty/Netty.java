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

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Netty extends Server.Base {

  private List<Jooby> applications = new ArrayList<>();

  private boolean SSL = false;

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

  @Nonnull @Override public Server deploy(Jooby application) {
    applications.add(application);
    return this;
  }

  @Nonnull @Override public Server start() {
    try {
      addShutdownHook();

      fireStart(applications, () -> worker = new DefaultEventExecutorGroup(32));

      NettyNative provider = NettyNative.get();
      /** Acceptor: */
      this.acceptor = provider.group(1);
      /** Client: */
      this.ioLoop = provider.group(0);

      final SslContext sslCtx;
      if (SSL) {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
      } else {
        sslCtx = null;
      }

      /** Handler: */
      HttpDataFactory factory = new DefaultHttpDataFactory(bufferSize);
      Supplier<ChannelInboundHandler> handler;

      if (applications.size() == 1) {
        handler = () -> new NettyHandler(applications.get(0), maxRequestSize, factory);
      } else {
        handler = () -> new NettyMultiHandler(applications);
      }

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 10000);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new NettyPipeline(sslCtx, handler, gzip, maxRequestSize))
          .childOption(ChannelOption.SO_REUSEADDR, true);

      bootstrap.bind(port).sync();
    } catch (CertificateException | SSLException x) {
      throw Throwing.sneakyThrow(x);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }

    fireReady(applications);

    /** Disk attributes: */
    if (applications.size() == 1) {
      String tmpdir = applications.get(0).tmpdir().toString();
      DiskFileUpload.baseDirectory = tmpdir;
      DiskAttribute.baseDirectory = tmpdir;
    } else {
      DiskFileUpload.baseDirectory = null; // system temp directory
      DiskAttribute.baseDirectory = null; // system temp directory
    }

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
