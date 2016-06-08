package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;

import org.jooby.spi.HttpHandler;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyHandler.class, NettyRequest.class, NettyResponse.class,
    DefaultFullHttpResponse.class, HttpUtil.class })
public class NettyHandlerTest {

  private Block channel = unit -> {
    Channel channel = unit.mock(Channel.class);
    unit.registerMock(Channel.class, channel);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.channel()).andReturn(channel).times(1, 4);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0With100ContinueExpected() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class,
        FullHttpRequest.class)
            .expect(channel)
            .expect(unit -> {

              FullHttpRequest req = unit.get(FullHttpRequest.class);
              expect(req.uri()).andReturn("/");
              expect(req.method()).andReturn(HttpMethod.GET);

              ChannelFuture future = unit.mock(ChannelFuture.class);

              unit.mockStatic(HttpUtil.class);
              expect(HttpUtil.is100ContinueExpected(req)).andReturn(true);

              Attribute<Boolean> needFlush = unit.mock(Attribute.class);
              needFlush.set(true);

              Channel channel = unit.get(Channel.class);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

              DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
                  new Class[]{HttpVersion.class, HttpResponseStatus.class },
                  HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
              expect(ctx.write(rsp)).andReturn(future);

              Attribute<String> attr = unit.mock(Attribute.class);
              attr.set("GET /");

              expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
            })
            .expect(unit -> {
              Config config = unit.get(Config.class);
              expect(config.getString("application.tmpdir")).andReturn("target");
              expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
            })
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
                  .channelRead0(unit.get(ChannelHandlerContext.class),
                      unit.get(FullHttpRequest.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WithKeepAlive() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class,
        FullHttpRequest.class)
            .expect(channel)
            .expect(unit -> {

              FullHttpRequest req = unit.get(FullHttpRequest.class);
              expect(req.uri()).andReturn("/");
              expect(req.method()).andReturn(HttpMethod.GET);

              unit.mockStatic(HttpUtil.class);
              expect(HttpUtil.is100ContinueExpected(req)).andReturn(true);

              ChannelFuture future = unit.mock(ChannelFuture.class);

              Attribute<Boolean> needFlush = unit.mock(Attribute.class);
              needFlush.set(true);

              Channel channel = unit.get(Channel.class);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

              DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
                  new Class[]{HttpVersion.class, HttpResponseStatus.class },
                  HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
              expect(ctx.write(rsp)).andReturn(future);

              Attribute<String> attr = unit.mock(Attribute.class);
              attr.set("GET /");

              expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
            })
            .expect(unit -> {
              Config config = unit.get(Config.class);
              expect(config.getString("application.tmpdir")).andReturn("target");
              expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
            })
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
                  .channelRead0(unit.get(ChannelHandlerContext.class),
                      unit.get(FullHttpRequest.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteFlush() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Attribute<Boolean> needFlush = unit.mock(Attribute.class);
          expect(needFlush.get()).andReturn(true);

          Channel channel = unit.get(Channel.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

          expect(ctx.flush()).andReturn(ctx);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .channelReadComplete(unit.get(ChannelHandlerContext.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteNoFlush() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Attribute<Boolean> needFlush = unit.mock(Attribute.class);
          expect(needFlush.get()).andReturn(false);

          Channel channel = unit.get(Channel.class);

          expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .channelReadComplete(unit.get(ChannelHandlerContext.class));
        });
  }

  @Test
  public void channelReadCompleteDefFlush() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Channel channel = unit.get(Channel.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(null);

          expect(ctx.flush()).andReturn(ctx);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .channelReadComplete(unit.get(ChannelHandlerContext.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WebSocket() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class, WebSocketFrame.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          NettyWebSocket ws = unit.mock(NettyWebSocket.class);
          ws.handle(unit.get(WebSocketFrame.class));

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(ws);

          Channel channel = unit.get(Channel.class);
          expect(channel.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .channelRead0(unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketFrame.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WithException() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class,
        FullHttpRequest.class)
            .expect(channel)
            .expect(unit -> {
              FullHttpRequest req = unit.get(FullHttpRequest.class);
              expect(req.uri()).andReturn("/");
              expect(req.method()).andReturn(HttpMethod.GET);

              unit.mockStatic(HttpUtil.class);
              expect(HttpUtil.is100ContinueExpected(req)).andReturn(false);
              expect(HttpUtil.isKeepAlive(req)).andReturn(true);

              Attribute<Boolean> needFlush = unit.mock(Attribute.class);
              needFlush.set(true);

              Channel channel = unit.get(Channel.class);

              expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

              Attribute<String> attr = unit.mock(Attribute.class);
              attr.set("GET /");
              expect(attr.get()).andReturn("GET /");

              expect(channel.attr(NettyHandler.PATH)).andReturn(attr).times(2);
            })
            .expect(unit -> {
              Config config = unit.get(Config.class);
              expect(config.getString("application.tmpdir")).andReturn("target");
              expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
              expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);

              NettyRequest req = unit.mockConstructor(NettyRequest.class,
                  new Class[]{ChannelHandlerContext.class, HttpRequest.class, String.class,
                      int.class },
                  ctx, unit.get(FullHttpRequest.class), "target", 3000);
              NettyResponse rsp = unit.mockConstructor(NettyResponse.class,
                  new Class[]{ChannelHandlerContext.class, int.class, boolean.class }, ctx, 8192,
                  true);

              HttpHandler dispatcher = unit.get(HttpHandler.class);
              dispatcher.handle(req, rsp);
              expectLastCall().andThrow(new Exception("intentional err"));
            })
            .expect(unit -> {
              ChannelFuture future = unit.mock(ChannelFuture.class);

              Channel channel = unit.get(Channel.class);
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(channel.attr(NettyWebSocket.KEY)).andReturn(null);
              expect(ctx.close()).andReturn(future);
            })
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
                  .channelRead0(unit.get(ChannelHandlerContext.class),
                      unit.get(FullHttpRequest.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtWebSocket() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          NettyWebSocket ws = unit.mock(NettyWebSocket.class);
          ws.handle(cause);

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(ws).times(2);

          Channel channel = unit.get(Channel.class);
          expect(channel.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtNormalNullWsAttr() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(null);

          Channel channel = unit.get(Channel.class);

          expect(channel.attr(NettyWebSocket.KEY)).andReturn(attr);

          Attribute<String> attr1 = unit.mock(Attribute.class);
          expect(attr1.get()).andReturn("GET /");

          expect(channel.attr(NettyHandler.PATH)).andReturn(attr1);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtNormalNullNullWsAttr() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          Channel channel = unit.get(Channel.class);

          expect(channel.attr(NettyWebSocket.KEY)).andReturn(null);

          Attribute<String> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn("GET /");

          expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtConnectionResetByPeerNoIOEx() throws Exception {
    Exception cause = new Exception("Connection reset by peer  (intentional error)");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          Channel channel = unit.get(Channel.class);

          expect(channel.attr(NettyWebSocket.KEY)).andReturn(null);

          Attribute<String> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn("GET /");

          expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtIOEx() throws Exception {
    Exception cause = new IOException("Connection reset by pexer");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          Channel channel = unit.get(Channel.class);
          expect(channel.attr(NettyWebSocket.KEY)).andReturn(null);

          Attribute<String> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn("GET /");

          expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtConnectionResetByPeer() throws Exception {
    Exception cause = new IOException("Connection reset by peer (intentional error)");
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(channel)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);

          Channel channel = unit.get(Channel.class);

          Attribute<String> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn("GET /");

          expect(channel.attr(NettyHandler.PATH)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @Test
  public void idleState() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .userEventTriggered(unit.get(ChannelHandlerContext.class),
                  IdleStateEvent.ALL_IDLE_STATE_EVENT);
        });
  }

  @Test
  public void userTriggeredEvent() throws Exception {
    Object evt = new Object();
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");
          expect(config.getBytes("server.ws.MaxTextMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.ws.MaxBinaryMessageSize")).andReturn(3000L);
          expect(config.getBytes("server.http.ResponseBufferSize")).andReturn(8192L);
        })
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.fireUserEventTriggered(evt)).andReturn(ctx);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), unit.get(Config.class))
              .userEventTriggered(unit.get(ChannelHandlerContext.class), evt);
        });
  }
}
