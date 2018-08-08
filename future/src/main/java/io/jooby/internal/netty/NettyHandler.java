package io.jooby.internal.netty;

import io.jooby.Route;
import io.jooby.Router;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

@ChannelHandler.Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final DefaultEventExecutorGroup executor;
  private final Router router;

  public NettyHandler(DefaultEventExecutorGroup executor, Router router) {
    this.executor = executor;
    this.router = router;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      String path = req.uri();
      int q = path.indexOf('?');
      if (q > 0) {
        path = path.substring(0, q);
      }
      String method = req.method().asciiName().toUpperCase().toString();
      Route route = router.match(method, path);
      Route.RootHandler handler = router.asRootHandler(route.pipeline());
      handler.apply(new NettyContext(ctx, executor, req, isKeepAlive(req), path, route));
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    //    if (!ConnectionLost.test(cause)) {
    //      String path = ctx.channel().attr(PATH).get();
    //      ctx.close();
    //      LoggerFactory.getLogger(Err.class)
    //          .error("execution of {} resulted in unexpected exception", path, cause);
    //    }
  }
}

