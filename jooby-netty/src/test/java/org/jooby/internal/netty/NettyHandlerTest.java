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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http2.HttpConversionUtil;
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
              HttpHeaders headers = unit.mock(HttpHeaders.class);
              expect(headers.get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text()))
                  .andReturn(null);

              FullHttpRequest req = unit.get(FullHttpRequest.class);
              expect(req.uri()).andReturn("/");
              expect(req.method()).andReturn(HttpMethod.GET);
              expect(req.headers()).andReturn(headers);

              unit.mockStatic(HttpUtil.class);
              expect(HttpUtil.is100ContinueExpected(req)).andReturn(true);
              ChannelFuture future = unit.mock(ChannelFuture.class);
              DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
                  new Class[]{HttpVersion.class, HttpResponseStatus.class },
                  HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
              expect(unit.get(ChannelHandlerContext.class).write(rsp)).andReturn(future);

              expect(HttpUtil.isKeepAlive(req)).andReturn(true);

              Attribute<Boolean> needFlush = unit.mock(Attribute.class);
              needFlush.set(true);

              Channel channel = unit.get(Channel.class);

              expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

              Attribute<String> attr = unit.mock(Attribute.class);
              attr.set("GET /");

              expect(channel.attr(NettyHandler.PATH)).andReturn(attr);

              NettyRequest nreq = unit.constructor(NettyRequest.class)
                  .args(ChannelHandlerContext.class, HttpRequest.class, String.class, int.class)
                  .build(unit.get(ChannelHandlerContext.class), req, "target", 3000);

              NettyResponse nrsp = unit.constructor(NettyResponse.class)
                  .args(ChannelHandlerContext.class, int.class, boolean.class, String.class)
                  .build(unit.get(ChannelHandlerContext.class), 8192, true, null);

              unit.get(HttpHandler.class).handle(nreq, nrsp);
            })
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
                  .channelRead0(unit.get(ChannelHandlerContext.class),
                      unit.get(FullHttpRequest.class));
            });
  }

  @Test
  public void channelRead0Ignored() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class,
        FullHttpRequest.class)
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
                  .channelRead0(unit.get(ChannelHandlerContext.class),
                      new Object());
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WithKeepAlive() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class,
        FullHttpRequest.class)
            .expect(channel)
            .expect(unit -> {

              HttpHeaders headers = unit.mock(HttpHeaders.class);
              expect(headers.get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text()))
                  .andReturn(null);

              FullHttpRequest req = unit.get(FullHttpRequest.class);
              expect(req.uri()).andReturn("/");
              expect(req.method()).andReturn(HttpMethod.GET);
              expect(req.headers()).andReturn(headers);

              unit.mockStatic(HttpUtil.class);
              expect(HttpUtil.is100ContinueExpected(req)).andReturn(false);
              expect(HttpUtil.isKeepAlive(req)).andReturn(true);

              Attribute<Boolean> needFlush = unit.mock(Attribute.class);
              needFlush.set(true);

              Channel channel = unit.get(Channel.class);

              expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);

              Attribute<String> attr = unit.mock(Attribute.class);
              attr.set("GET /");

              expect(channel.attr(NettyHandler.PATH)).andReturn(attr);

              NettyRequest nreq = unit.constructor(NettyRequest.class)
                  .args(ChannelHandlerContext.class, HttpRequest.class, String.class, int.class)
                  .build(unit.get(ChannelHandlerContext.class), req, "target", 3000);

              NettyResponse nrsp = unit.constructor(NettyResponse.class)
                  .args(ChannelHandlerContext.class, int.class, boolean.class, String.class)
                  .build(unit.get(ChannelHandlerContext.class), 8192, true, null);

              unit.get(HttpHandler.class).handle(nreq, nrsp);
            })
            .run(unit -> {
              new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
              .channelReadComplete(unit.get(ChannelHandlerContext.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WebSocket() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class, WebSocketFrame.class)
        .expect(channel)
        .expect(unit -> {
          NettyWebSocket ws = unit.mock(NettyWebSocket.class);
          ws.handle(unit.get(WebSocketFrame.class));

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(ws);

          Channel channel = unit.get(Channel.class);
          expect(channel.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);

              HttpHeaders headers = unit.mock(HttpHeaders.class);
              expect(headers.get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text()))
                  .andReturn(null);

              FullHttpRequest request = unit.get(FullHttpRequest.class);
              expect(request.headers()).andReturn(headers);

              NettyRequest req = unit.constructor(NettyRequest.class)
                  .args(ChannelHandlerContext.class, HttpRequest.class, String.class, int.class)
                  .build(ctx, request, "target", 3000);

              NettyResponse rsp = unit.constructor(NettyResponse.class)
                  .args(ChannelHandlerContext.class, int.class, boolean.class, String.class)
                  .build(ctx, 8192, true, null);

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
              new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
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
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @Test
  public void idleState() throws Exception {
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
              .userEventTriggered(unit.get(ChannelHandlerContext.class),
                  IdleStateEvent.ALL_IDLE_STATE_EVENT);
        });
  }

  @Test
  public void userTriggeredEvent() throws Exception {
    Object evt = new Object();
    new MockUnit(HttpHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.fireUserEventTriggered(evt)).andReturn(ctx);
        })
        .run(unit -> {
          new NettyHandler(unit.get(HttpHandler.class), "target", 8192, 3000)
              .userEventTriggered(unit.get(ChannelHandlerContext.class), evt);
        });
  }
}
