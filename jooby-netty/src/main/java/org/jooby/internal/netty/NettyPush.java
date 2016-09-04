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

import java.util.Map;

import org.jooby.spi.NativePushPromise;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.util.AsciiString;

public class NettyPush implements NativePushPromise {

  private ChannelHandlerContext ctx;

  private Http2ConnectionEncoder encoder;

  private int streamId;

  private String authority;

  private String scheme;

  public NettyPush(final ChannelHandlerContext ctx, final int streamId, final String authority,
      final String scheme) {
    this.ctx = ctx;
    HttpToHttp2ConnectionHandler handler = ctx.pipeline().get(HttpToHttp2ConnectionHandler.class);
    this.encoder = handler.encoder();
    this.streamId = streamId;
    this.authority = authority;
    this.scheme = scheme;
  }

  @Override
  public void push(final String method, final String path, final Map<String, Object> headers) {
    ctx.channel().eventLoop().execute(() -> {
      AsciiString streamIdHeader = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();
      Http2Connection connection = encoder.connection();
      int nextStreamId = connection.local().incrementAndGetNextStreamId();
      Http2Headers h2headers = new DefaultHttp2Headers()
          .path(path)
          .method(method)
          .authority(authority)
          .scheme(scheme);
      headers.forEach((n, v) -> h2headers.add(n, v.toString()));
      encoder.writePushPromise(ctx, streamId, nextStreamId, h2headers, 0, ctx.newPromise());

      // TODO: Is there another way of handling a push promise?
      DefaultFullHttpRequest pushRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
          HttpMethod.valueOf(method.toUpperCase()), path, Unpooled.EMPTY_BUFFER,
          new DefaultHttpHeaders(false).set(streamIdHeader, nextStreamId),
          EmptyHttpHeaders.INSTANCE);
      ctx.pipeline().fireChannelRead(pushRequest);
      ctx.pipeline().fireChannelReadComplete();
    });
  }

}
