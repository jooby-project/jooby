package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;

import org.jooby.MockUnit;
import org.jooby.spi.ApplicationHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyHandler.class, NettyRequest.class, NettyResponse.class,
    DefaultFullHttpResponse.class })
public class NettyHandlerTest {

  @Test
  public void channelReadComplete() throws Exception {
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.flush()).andReturn(ctx);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .channelReadComplete(unit.get(ChannelHandlerContext.class));
        });
  }

  @Test
  public void channelReadCompleteRead0With100ContinueExpected() throws Exception {
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class, FullHttpRequest.class)
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Expect")).andReturn("100-Continue");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.getProtocolVersion()).andReturn(HttpVersion.HTTP_1_1);
          expect(req.headers()).andReturn(headers);

          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
          expect(ctx.write(rsp)).andReturn(future);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Connection")).andReturn("Close");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.headers()).andReturn(headers);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);

          NettyRequest req = unit.mockConstructor(NettyRequest.class,
              new Class[]{ChannelHandlerContext.class, HttpRequest.class, String.class },
              ctx, unit.get(FullHttpRequest.class), "target");
          NettyResponse rsp = unit.mockConstructor(NettyResponse.class,
              new Class[]{ChannelHandlerContext.class, NettyRequest.class, boolean.class },
              ctx, req, false);

          ApplicationHandler dispatcher = unit.get(ApplicationHandler.class);
          dispatcher.handle(req, rsp);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .channelRead0(unit.get(ChannelHandlerContext.class),
                  unit.get(FullHttpRequest.class));
        });
  }

  @Test
  public void channelReadCompleteRead0WithKeepAlive() throws Exception {
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class, FullHttpRequest.class)
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Expect")).andReturn("100-Continue");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.getProtocolVersion()).andReturn(HttpVersion.HTTP_1_1);
          expect(req.headers()).andReturn(headers);

          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
          expect(ctx.write(rsp)).andReturn(future);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Connection")).andReturn("Keep-Alive");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.getProtocolVersion()).andReturn(HttpVersion.HTTP_1_1);
          expect(req.headers()).andReturn(headers);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);

          NettyRequest req = unit.mockConstructor(NettyRequest.class,
              new Class[]{ChannelHandlerContext.class, HttpRequest.class, String.class },
              ctx, unit.get(FullHttpRequest.class), "target");
          NettyResponse rsp = unit.mockConstructor(NettyResponse.class,
              new Class[]{ChannelHandlerContext.class, NettyRequest.class, boolean.class },
              ctx, req, true);

          ApplicationHandler dispatcher = unit.get(ApplicationHandler.class);
          dispatcher.handle(req, rsp);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .channelRead0(unit.get(ChannelHandlerContext.class),
                  unit.get(FullHttpRequest.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void channelReadCompleteRead0WebSocket() throws Exception {
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class, WebSocketFrame.class)
        .expect(unit -> {
          NettyWebSocket ws = unit.mock(NettyWebSocket.class);
          ws.handle(unit.get(WebSocketFrame.class));

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(ws);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .channelRead0(unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketFrame.class));
        });
  }

  @Test
  public void channelReadCompleteRead0WithException() throws Exception {
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class, FullHttpRequest.class)
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Expect")).andReturn("100-Continue");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.getProtocolVersion()).andReturn(HttpVersion.HTTP_1_1);
          expect(req.headers()).andReturn(headers);

          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
          expect(ctx.write(rsp)).andReturn(future);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.get("Connection")).andReturn("Keep-Alive");

          FullHttpRequest req = unit.get(FullHttpRequest.class);
          expect(req.getProtocolVersion()).andReturn(HttpVersion.HTTP_1_1);
          expect(req.headers()).andReturn(headers);
        })
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("application.tmpdir")).andReturn("target");

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);

          NettyRequest req = unit.mockConstructor(NettyRequest.class,
              new Class[]{ChannelHandlerContext.class, HttpRequest.class, String.class },
              ctx, unit.get(FullHttpRequest.class), "target");
          NettyResponse rsp = unit.mockConstructor(NettyResponse.class,
              new Class[]{ChannelHandlerContext.class, NettyRequest.class, boolean.class },
              ctx, req, true);

          ApplicationHandler dispatcher = unit.get(ApplicationHandler.class);
          dispatcher.handle(req, rsp);
          expectLastCall().andThrow(new Exception("intentional err"));
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .channelRead0(unit.get(ChannelHandlerContext.class),
                  unit.get(FullHttpRequest.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtWebSocket() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          NettyWebSocket ws = unit.mock(NettyWebSocket.class);
          ws.handle(cause);

          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(ws).times(2);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void exceptionCaughtNormalNullWsAttr() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(null);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }

  @Test
  public void exceptionCaughtNormalNullNullWsAttr() throws Exception {
    Exception cause = new Exception("intentional error");
    new MockUnit(ApplicationHandler.class, Config.class, ChannelHandlerContext.class)
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
        })
        .expect(unit -> {
          ChannelFuture future = unit.mock(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.close()).andReturn(future);
        })
        .run(unit -> {
          new NettyHandler(unit.get(ApplicationHandler.class), unit.get(Config.class))
              .exceptionCaught(unit.get(ChannelHandlerContext.class), cause);
        });
  }
}
