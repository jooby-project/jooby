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

import io.jooby.Jooby;
import io.jooby.Router;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.List;
import java.util.Map;

@ChannelHandler.Sharable
public class NettyMultiHandler extends ChannelInboundHandlerAdapter {
  private final List<Jooby> routers;

  public NettyMultiHandler(List<Jooby> routers) {
    this.routers = routers;
  }

  @Override public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      String uri = request.uri();
      String path = NettyHandler.pathOnly(uri);
      for (Jooby router : routers) {
        NettyContext context = new NettyContext(ctx, request, router, path);
        Router.Match match = router.match(context);
        if (match.matches()) {
          match.execute(context);
          return;
        }
      }
    } else {
      ctx.fireChannelRead(ctx);
    }
  }

}
