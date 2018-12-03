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
package io.jooby.internal.netty;

import io.jooby.Route;
import io.jooby.Router;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@ChannelHandler.Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {
  private final Router router;

  public NettyHandler(Router router) {
    this.router = router;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      NettyContext context = new NettyContext(ctx, router.worker(), req,
          router.errorHandler(),
          router.tmpdir(), pathOnly(req.uri()));
      handleHttpRequest(ctx, req, context);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req, NettyContext context)
      throws Exception {
    Router.Match match = router.match(context);
    Route route = match.route();
    if (route.gzip() && acceptGzip(req.headers().get(HttpHeaderNames.ACCEPT_ENCODING))) {
      installGzip(ctx, req);
    }
    Route.Handler handler = route.pipeline();
    handler.execute(context);
  }

  private static boolean acceptGzip(String value) {
    return value != null && value.contains("gzip");
  }

  private static void installGzip(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
    HttpContentCompressor compressor = new HttpContentCompressor() {
      @Override
      protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
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

  static String pathOnly(String uri) {
    int len = uri.length();
    for (int i = 0; i < len; i++) {
      char c = uri.charAt(i);
      if (c == '?') {
        return uri.substring(0, i);
      }
    }
    return uri;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
    //    if (!ConnectionLost.test(cause)) {
    //      String path = ctx.channel().attr(PATH).get();
    //      ctx.close();
    //      LoggerFactory.getLogger(Err.class)
    //          .error("execution of {} resulted in unexpected exception", path, cause);
    //    }
  }
}

