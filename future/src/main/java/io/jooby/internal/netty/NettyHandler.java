package io.jooby.internal.netty;

import io.jooby.Route;
import io.jooby.Router;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.List;

@ChannelHandler.Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final DefaultEventExecutorGroup executor;
  private final Router router;

  public NettyHandler(DefaultEventExecutorGroup executor, Router router) {
    this.executor = executor;
    this.router = router;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      String path = req.uri();
      int q = path.indexOf('?');
      if (q > 0) {
        path = path.substring(0, q);
      }
      NettyContext context = new NettyContext(ctx, executor, req, router.errorHandler(), path);
      Router.Match match = router.match(context);
      Route route = match.route();
      if (route.gzip() && req.headers().contains(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP, true)) {
        installGzip(ctx, req);
      }
      Route.RootHandler handler = route.pipeline();
      handler.apply(context);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private static void installGzip(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
    HttpContentCompressor compressor = new HttpContentCompressor() {
      @Override protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
          throws Exception {
        super.encode(ctx, msg, out);
        // TODO: is there a better way?
        if (msg instanceof LastHttpContent) {
          ctx.pipeline().remove(this);
        }
      }
    };
    compressor.channelRead(ctx, req);
    // Initialize
    ctx.pipeline().addBefore("handler", "gzip", compressor);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    //    cause.printStackTrace();
    ctx.close();
    //    if (!ConnectionLost.test(cause)) {
    //      String path = ctx.channel().attr(PATH).get();
    //      ctx.close();
    //      LoggerFactory.getLogger(Err.class)
    //          .error("execution of {} resulted in unexpected exception", path, cause);
    //    }
  }
}

