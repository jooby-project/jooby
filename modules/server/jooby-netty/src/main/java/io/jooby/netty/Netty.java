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
import io.jooby.Functions;
import io.jooby.internal.netty.NettyMultiHandler;
import io.jooby.internal.netty.NettyNative;
import io.jooby.internal.netty.NettyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Netty implements Server {

  private static class Pipeline extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final ChannelInboundHandler handler;
    private final boolean gzip;

    public Pipeline(SslContext sslCtx, ChannelInboundHandler handler, boolean gzip) {
      this.sslCtx = sslCtx;
      this.handler = handler;
      this.gzip = gzip;
    }

    @Override
    public void initChannel(SocketChannel ch) {
      ChannelPipeline p = ch.pipeline();
      if (sslCtx != null) {
        p.addLast(sslCtx.newHandler(ch.alloc()));
      }
      // FIXME: check configuration parameters
      p.addLast("codec", new HttpServerCodec());
      p.addLast("aggregator", new HttpObjectAggregator(_16KB * 2));
      if (gzip) {
        p.addLast("gzip", new HttpContentCompressor());
      }
      p.addLast("handler", handler);
    }
  }

  private List<Jooby> applications = new ArrayList<>();

  private boolean SSL = false;

  private EventLoopGroup acceptor;

  private EventLoopGroup ioLoop;

  private int port = 8080;

  private boolean gzip;

  private DefaultEventExecutorGroup worker;

  @Override public Server port(int port) {
    this.port = port;
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
      ChannelInboundHandler handler;
      if (applications.size() == 1) {
        handler = new NettyHandler(applications.get(0));
      } else {
        Map<Jooby, NettyHandler> handlers = new LinkedHashMap<>(applications.size());
        applications.forEach(app -> handlers.put(app, new NettyHandler(app)));
        handler = new NettyMultiHandler(handlers, worker);
      }

      /** Bootstrap: */
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.option(ChannelOption.SO_BACKLOG, 10000);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);

      bootstrap.group(acceptor, ioLoop)
          .channel(provider.channel())
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new Pipeline(sslCtx, handler, gzip))
          .childOption(ChannelOption.SO_REUSEADDR, true);

      bootstrap.bind(port).sync();
    } catch (CertificateException | SSLException x) {
      throw Throwing.sneakyThrow(x);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }

    applications.forEach(app -> {
      app.worker(Optional.ofNullable(app.worker()).orElseGet(() -> {
        if (worker == null) {
          worker = new DefaultEventExecutorGroup(32);
        }
        return worker;
      }));
      app.start(this);
    });

    return this;
  }

  public Server stop() {
    try (Functions.Closer closer = Functions.closer()) {
      applications.forEach(app -> closer.register(app::stop));
      closer.register(ioLoop::shutdownGracefully);
      closer.register(acceptor::shutdownGracefully);
      if (worker != null) {
        closer.register(worker::shutdownGracefully);
      }
    }
    return this;
  }

}
