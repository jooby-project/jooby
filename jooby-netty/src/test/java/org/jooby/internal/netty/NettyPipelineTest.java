package org.jooby.internal.netty;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.jooby.internal.netty.NettyPipeline.Http2OrHttpHandler;
import org.jooby.internal.netty.NettyPipeline.Http2PrefaceOrHttpHandler;
import org.jooby.spi.HttpHandler;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.SourceCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyPipeline.class, SslContext.class, HttpServerCodec.class,
    IdleStateHandler.class, InboundHttp2ToHttpAdapterBuilder.class,
    HttpToHttp2ConnectionHandlerBuilder.class })
public class NettyPipelineTest {

  private Block pipeline = unit -> {
    SocketChannel channel = unit.get(SocketChannel.class);
    expect(channel.pipeline()).andReturn(unit.get(ChannelPipeline.class));
  };

  private Block ctxpipeline = unit -> {
    ChannelHandlerContext channel = unit.get(ChannelHandlerContext.class);
    expect(channel.pipeline()).andReturn(unit.get(ChannelPipeline.class));
  };

  private Block ssl = unit -> {
    ByteBufAllocator bufalloc = unit.mock(ByteBufAllocator.class);

    SocketChannel channel = unit.get(SocketChannel.class);
    expect(channel.alloc()).andReturn(bufalloc);

    SslHandler handler = unit.mock(SslHandler.class);

    SslContext sslContext = unit.get(SslContext.class);
    expect(sslContext.newHandler(bufalloc)).andReturn(handler);

    ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
    expect(pipeline.addLast("ssl", handler)).andReturn(pipeline);
  };

  private Block sslContext = unit -> {
    SslContext sslcontext = unit.powerMock(SslContext.class);
    unit.registerMock(SslContext.class, sslcontext);
  };

  private Block http2OrHttp = unit -> {
    ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
    expect(pipeline.addLast(eq("h1.1/h2"), unit.capture(Http2OrHttpHandler.class)))
        .andReturn(pipeline);
  };

  @Test
  public void https1_1() throws Exception {
    Config conf = conf(false, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(sslContext)
            .expect(pipeline)
            .expect(ssl)
            .expect(http2OrHttp)
            .expect(ctxpipeline)
            .expect(http1Codec())
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, unit.get(SslContext.class))
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2OrHttpHandler handler = unit.captured(Http2OrHttpHandler.class).iterator()
                  .next();
              handler.configurePipeline(unit.get(ChannelHandlerContext.class), "http/1.1");
            });
  }

  @Test
  public void http1_1() throws Exception {
    Config conf = conf(false, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(http1Codec())
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            });
  }

  @Test
  public void h2cDirect() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(unit -> {
              ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
              expect(pipeline.addLast(eq("h2c"), unit.capture(Http2PrefaceOrHttpHandler.class)))
                  .andReturn(pipeline);
            })
            .expect(ctxpipeline)
            .expect(ctxpipeline)
            .expect(h2(456, (u, h2) -> {
              ChannelPipeline p = u.get(ChannelPipeline.class);
              expect(p.addAfter(null, "h2", h2)).andReturn(p);
              ChannelHandlerContext ctx = u.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("h2");
              expect(p.context(h2)).andReturn(ctx);
              expect(p.remove(isA(Http2PrefaceOrHttpHandler.class))).andReturn(p);
            }))
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2PrefaceOrHttpHandler handler = unit.captured(Http2PrefaceOrHttpHandler.class)
                  .iterator()
                  .next();
              handler.decode(unit.get(ChannelHandlerContext.class),
                  Unpooled.wrappedBuffer("PRI HTTP".getBytes(StandardCharsets.UTF_8)), null);
            });
  }

  @Test
  public void h2prefaceIgnored() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(unit -> {
              ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
              expect(pipeline.addLast(eq("h2c"), unit.capture(Http2PrefaceOrHttpHandler.class)))
                  .andReturn(pipeline);
            })
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2PrefaceOrHttpHandler handler = unit.captured(Http2PrefaceOrHttpHandler.class)
                  .iterator()
                  .next();
              handler.decode(unit.get(ChannelHandlerContext.class),
                  Unpooled.wrappedBuffer("123".getBytes(StandardCharsets.UTF_8)), null);
            });
  }

  @Test
  public void httpToh2c() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(unit -> {
              ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
              expect(pipeline.addLast(eq("h2c"), unit.capture(Http2PrefaceOrHttpHandler.class)))
                  .andReturn(pipeline);
            })
            .expect(ctxpipeline)
            .expect(ctxpipeline)
            .expect(http1Codec((p, h) -> {
              expect(p.addAfter(null, "codec", h)).andReturn(p);
            }))
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("codec");
              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.context(unit.get(HttpServerCodec.class))).andReturn(ctx);
            })
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("h2upgrade");

              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.addAfter(eq("codec"), eq("h2upgrade"),
                  unit.capture(HttpServerUpgradeHandler.class)))
                      .andReturn(p);
              expect(p.context(isA(HttpServerUpgradeHandler.class))).andReturn(ctx);

              expect(p.remove(isA(Http2PrefaceOrHttpHandler.class))).andReturn(p);
            })
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2PrefaceOrHttpHandler handler = unit.captured(Http2PrefaceOrHttpHandler.class)
                  .iterator()
                  .next();
              handler.decode(unit.get(ChannelHandlerContext.class),
                  Unpooled.wrappedBuffer("GET HTTP".getBytes(StandardCharsets.UTF_8)), null);
            });
  }

  @Test
  public void httpToh2cIgnoreUpgrade() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(unit -> {
              ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
              expect(pipeline.addLast(eq("h2c"), unit.capture(Http2PrefaceOrHttpHandler.class)))
                  .andReturn(pipeline);
            })
            .expect(ctxpipeline)
            .expect(ctxpipeline)
            .expect(http1Codec((p, h) -> {
              expect(p.addAfter(null, "codec", h)).andReturn(p);
            }))
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("codec");
              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.context(unit.get(HttpServerCodec.class))).andReturn(ctx);
            })
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("h2upgrade");

              HttpServerCodec http1Codec = unit.get(HttpServerCodec.class);

              HttpServerUpgradeHandler h = unit.constructor(HttpServerUpgradeHandler.class)
                  .args(SourceCodec.class, UpgradeCodecFactory.class, int.class)
                  .build(eq(http1Codec), unit.capture(UpgradeCodecFactory.class), eq(456));

              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.addAfter("codec", "h2upgrade", h)).andReturn(p);
              expect(p.context(isA(HttpServerUpgradeHandler.class))).andReturn(ctx);

              expect(p.remove(isA(Http2PrefaceOrHttpHandler.class))).andReturn(p);
            })
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2PrefaceOrHttpHandler handler = unit.captured(Http2PrefaceOrHttpHandler.class)
                  .iterator()
                  .next();
              handler.decode(unit.get(ChannelHandlerContext.class),
                  Unpooled.wrappedBuffer("GET HTTP".getBytes(StandardCharsets.UTF_8)), null);
            }, unit -> {
              UpgradeCodecFactory u = unit.captured(UpgradeCodecFactory.class).iterator().next();
              assertEquals(null, u.newUpgradeCodec("http/1.1"));
            });
  }

  @Test
  public void httpToh2cSuccessUpgrade() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(pipeline)
            .expect(unit -> {
              ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
              expect(pipeline.addLast(eq("h2c"), unit.capture(Http2PrefaceOrHttpHandler.class)))
                  .andReturn(pipeline);
            })
            .expect(ctxpipeline)
            .expect(ctxpipeline)
            .expect(http1Codec((p, h) -> {
              expect(p.addAfter(null, "codec", h)).andReturn(p);
            }))
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("codec");
              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.context(unit.get(HttpServerCodec.class))).andReturn(ctx);
            })
            .expect(unit -> {
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.name()).andReturn("h2upgrade");

              HttpServerCodec http1Codec = unit.get(HttpServerCodec.class);

              HttpServerUpgradeHandler h = unit.constructor(HttpServerUpgradeHandler.class)
                  .args(SourceCodec.class, UpgradeCodecFactory.class, int.class)
                  .build(eq(http1Codec), unit.capture(UpgradeCodecFactory.class), eq(456));

              ChannelPipeline p = unit.get(ChannelPipeline.class);
              expect(p.addAfter("codec", "h2upgrade", h)).andReturn(p);
              expect(p.context(isA(HttpServerUpgradeHandler.class))).andReturn(ctx);

              expect(p.remove(isA(Http2PrefaceOrHttpHandler.class))).andReturn(p);
            })
            .expect(idle(567))
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, null)
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2PrefaceOrHttpHandler handler = unit.captured(Http2PrefaceOrHttpHandler.class)
                  .iterator()
                  .next();
              handler.decode(unit.get(ChannelHandlerContext.class),
                  Unpooled.wrappedBuffer("GET HTTP".getBytes(StandardCharsets.UTF_8)), null);
            }, unit -> {
              UpgradeCodecFactory u = unit.captured(UpgradeCodecFactory.class).iterator().next();
              assertTrue(u.newUpgradeCodec("h2c") instanceof Http2ServerUpgradeCodec);
            });
  }

  private Block http1Codec() {
    return http1Codec((p, h) -> {
      expect(p.addLast("codec", h)).andReturn(p);
    });
  }

  private Block http1Codec(final BiConsumer<ChannelPipeline, HttpServerCodec> callback) {
    return unit -> {
      HttpServerCodec codec = unit.constructor(HttpServerCodec.class)
          .build(123, 234, 345, false);
      unit.registerMock(HttpServerCodec.class, codec);

      ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
      callback.accept(pipeline, codec);
    };
  }

  @Test
  public void h2() throws Exception {
    Config conf = conf(true, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(sslContext)
            .expect(pipeline)
            .expect(ssl)
            .expect(http2OrHttp)
            .expect(ctxpipeline)
            .expect(h2(456))
            .expect(idle(567))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, unit.get(SslContext.class))
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2OrHttpHandler handler = unit.captured(Http2OrHttpHandler.class).iterator()
                  .next();
              handler.configurePipeline(unit.get(ChannelHandlerContext.class), "h2");
            });
  }

  @Test
  public void https1_1_noTimeout() throws Exception {
    Config conf = conf(false, 123, 234, 345, 456, -1);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(sslContext)
            .expect(pipeline)
            .expect(ssl)
            .expect(http2OrHttp)
            .expect(ctxpipeline)
            .expect(http1Codec())
            .expect(aggregator(456))
            .expect(jooby(conf))
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, unit.get(SslContext.class))
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2OrHttpHandler handler = unit.captured(Http2OrHttpHandler.class).iterator()
                  .next();
              handler.configurePipeline(unit.get(ChannelHandlerContext.class), "http/1.1");
            });
  }

  @Test
  public void unknownProtocol() throws Exception {
    Config conf = conf(false, 123, 234, 345, 456, 567L);
    new MockUnit(EventExecutorGroup.class, HttpHandler.class, SocketChannel.class,
        ChannelPipeline.class, ChannelHandlerContext.class)
            .expect(sslContext)
            .expect(pipeline)
            .expect(ssl)
            .expect(http2OrHttp)
            .run(unit -> {
              new NettyPipeline(unit.get(EventExecutorGroup.class), unit.get(HttpHandler.class),
                  conf, unit.get(SslContext.class))
                      .initChannel(unit.get(SocketChannel.class));
            }, unit -> {
              Http2OrHttpHandler handler = unit.captured(Http2OrHttpHandler.class).iterator()
                  .next();
              try {
                handler.configurePipeline(unit.get(ChannelHandlerContext.class), "h2");
                fail();
              } catch (IllegalStateException x) {
                assertEquals("Unknown protocol: h2", x.getMessage());
              }
            });
  }

  private Block jooby(final Config conf) {
    return unit -> {
      NettyHandler handler = unit.constructor(NettyHandler.class)
          .build(unit.get(HttpHandler.class), conf);
      unit.registerMock(NettyHandler.class, handler);

      ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
      expect(pipeline.addLast(unit.get(EventExecutorGroup.class), "jooby", handler))
          .andReturn(pipeline);
    };
  }

  private Block aggregator(final int len) {
    return unit -> {
      HttpObjectAggregator aggregator = unit.constructor(HttpObjectAggregator.class)
          .build(len);
      unit.registerMock(HttpObjectAggregator.class, aggregator);

      ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
      expect(pipeline.addLast("aggregator", aggregator)).andReturn(pipeline);
    };
  }

  private Block h2(final int l) {
    return h2(l, (u, h2) -> {
      ChannelPipeline pipeline = u.get(ChannelPipeline.class);
      expect(pipeline.addLast("h2", h2)).andReturn(pipeline);
    });
  }

  private Block h2(final int l,
      final BiConsumer<MockUnit, HttpToHttp2ConnectionHandler> handler) {
    return unit -> {

      DefaultHttp2Connection connection = unit.constructor(DefaultHttp2Connection.class)
          .build(true);

      InboundHttp2ToHttpAdapterBuilder builder = unit
          .constructor(InboundHttp2ToHttpAdapterBuilder.class)
          .build(connection);

      InboundHttp2ToHttpAdapter adapter = unit.mock(InboundHttp2ToHttpAdapter.class);
      Http2FrameLogger logger = unit.constructor(Http2FrameLogger.class)
          .build(LogLevel.DEBUG);

      expect(builder.propagateSettings(false)).andReturn(builder);
      expect(builder.validateHttpHeaders(false)).andReturn(builder);
      expect(builder.maxContentLength(l)).andReturn(builder);
      expect(builder.build()).andReturn(adapter);

      HttpToHttp2ConnectionHandler h2 = unit.mock(HttpToHttp2ConnectionHandler.class);

      HttpToHttp2ConnectionHandlerBuilder h2builder = unit
          .constructor(HttpToHttp2ConnectionHandlerBuilder.class)
          .build();
      expect(h2builder.frameListener(adapter)).andReturn(h2builder);
      expect(h2builder.frameLogger(logger)).andReturn(h2builder);
      expect(h2builder.connection(connection)).andReturn(h2builder);
      expect(h2builder.build()).andReturn(h2);

      handler.accept(unit, h2);
    };
  }

  private Block idle(final long timeout) {
    return unit -> {
      IdleStateHandler idle = unit.constructor(IdleStateHandler.class)
          .build(0L, 0L, timeout, TimeUnit.MILLISECONDS);
      unit.registerMock(IdleStateHandler.class, idle);

      ChannelPipeline pipeline = unit.get(ChannelPipeline.class);
      expect(pipeline.addLast("timeout", idle)).andReturn(pipeline);
    };
  }

  private Config conf(final boolean http2, final int i, final int j, final int k, final int l,
      final long m) {
    return ConfigFactory.empty()
        .withValue("netty.http.MaxInitialLineLength", ConfigValueFactory.fromAnyRef(i))
        .withValue("netty.http.MaxHeaderSize", ConfigValueFactory.fromAnyRef(j))
        .withValue("netty.http.MaxChunkSize", ConfigValueFactory.fromAnyRef(k))
        .withValue("netty.http.MaxContentLength", ConfigValueFactory.fromAnyRef(l))
        .withValue("netty.http.IdleTimeout", ConfigValueFactory.fromAnyRef(m))
        .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(http2));
  }
}
