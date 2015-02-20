package org.jooby.internal.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import org.jooby.spi.Dispatcher;

import com.typesafe.config.Config;

public class NettyHandler extends ChannelInboundHandlerAdapter {

  public static final AttributeKey<NettyWebSocket> WS =
      new AttributeKey<NettyWebSocket>("__native_ws_");

  private Dispatcher dispatcher;

  private Config config;

  public NettyHandler(final Dispatcher dispatcher, final Config config) {
    this.dispatcher = dispatcher;
    this.config = config;
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof FullHttpRequest) {
      FullHttpRequest req = (FullHttpRequest) msg;

      if (HttpHeaders.is100ContinueExpected(req)) {
        ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
      }
      boolean keepAlive = HttpHeaders.isKeepAlive(req);

      try {
        NettyRequest nreq = new NettyRequest(ctx, req, config.getString("application.tmpdir"));
        dispatcher.handle(nreq, new NettyResponse(ctx, nreq, keepAlive));
      } catch (Exception ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }
    if (msg instanceof WebSocketFrame) {
      Attribute<NettyWebSocket> ws = ctx.attr(WS);
      ws.get().handle(msg);
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    Attribute<NettyWebSocket> ws = ctx.attr(WS);
    if (ws != null && ws.get() != null) {
      ws.get().handle(cause);
    } else {
      // TODO: log me
      cause.printStackTrace();
    }
    ctx.close();
  }

}
