/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Objects.requireNonNull;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;

import org.jooby.spi.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

public class NettyHandler extends SimpleChannelInboundHandler<Object> {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HttpHandler handler;

  private Config config;

  public NettyHandler(final HttpHandler handler, final Config config) {
    this.handler = requireNonNull(handler, "Application handler is required.");
    this.config = requireNonNull(config, "Application config is required.");
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof FullHttpRequest) {
      FullHttpRequest req = (FullHttpRequest) msg;

      if (HttpHeaders.is100ContinueExpected(req)) {
        ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
      }
      boolean keepAlive = HttpHeaders.isKeepAlive(req);

      try {
        NettyRequest nreq = new NettyRequest(ctx, req, config.getString("application.tmpdir"));
        handler.handle(nreq, new NettyResponse(ctx, nreq, keepAlive));
      } catch (Throwable ex) {
        exceptionCaught(ctx, ex);
      }
    } else if (msg instanceof WebSocketFrame) {
      Attribute<NettyWebSocket> ws = ctx.attr(NettyWebSocket.KEY);
      ws.get().handle(msg);
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    try {
      Attribute<NettyWebSocket> ws = ctx.attr(NettyWebSocket.KEY);
      if (ws != null && ws.get() != null) {
        ws.get().handle(cause);
      } else {
        log.error("execution of: " + ctx + " resulted in error", cause);
      }
    } finally {
      ctx.close();
    }
  }

}
