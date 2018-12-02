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
