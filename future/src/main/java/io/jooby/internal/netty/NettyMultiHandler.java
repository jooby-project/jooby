package io.jooby.internal.netty;

import io.jooby.App;
import io.jooby.Router;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.Map;

@ChannelHandler.Sharable
public class NettyMultiHandler extends ChannelInboundHandlerAdapter {
  private final Map<App, NettyHandler> handlers;
  private final DefaultEventExecutorGroup executor;

  public NettyMultiHandler(Map<App, NettyHandler> handlers, DefaultEventExecutorGroup executor) {
    this.handlers = handlers;
    this.executor = executor;
  }

  @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      String uri = request.uri();
      String path = NettyHandler.pathOnly(uri);
      for (Map.Entry<App, NettyHandler> e : handlers.entrySet()) {
        App router = e.getKey();
        NettyContext context = new NettyContext(ctx, executor, request, router.errorHandler(),
            router.tmpdir(), path);
        Router.Match match = router.match(context);
        if (match.matches()) {
          e.getValue().handleHttpRequest(ctx, request, context);
        }
      }
    } else {
      ctx.fireChannelRead(ctx);
    }
  }

}
