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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;

public class NettyResponse implements NativeResponse {

  private ChannelHandlerContext ctx;

  private boolean keepAlive;

  HttpResponseStatus status = HttpResponseStatus.OK;

  HttpHeaders headers;

  private NettyOutputStream out;

  public NettyResponse(final ChannelHandlerContext ctx, final boolean keepAlive) {
    this.ctx = ctx;
    this.keepAlive = keepAlive;
    this.headers = new DefaultHttpHeaders();
  }

  @Override
  public List<String> headers(final String name) {
    List<String> headers = this.headers.getAll(name);
    return headers == null ? Collections.emptyList() : ImmutableList.copyOf(headers);
  }

  @Override
  public Optional<String> header(final String name) {
    String value = this.headers.get(name);
    return Optional.ofNullable(value);
  }

  @Override
  public void header(final String name, final String value) {
    headers.set(name, value);
  }

  @Override
  public void header(final String name, final Iterable<String> values) {
    headers.remove(name);
    headers.add(name, values);
  }

  @Override
  public OutputStream out(final int bufferSize) throws IOException {
    if (out == null) {
      out = new NettyOutputStream(this, ctx, Unpooled.buffer(0, bufferSize), keepAlive, headers);
    }
    return out;
  }

  @Override
  public int statusCode() {
    return status.code();
  }

  @Override
  public void statusCode(final int code) {
    this.status = HttpResponseStatus.valueOf(code);
  }

  @Override
  public boolean committed() {
    if (ctx == null) {
      return true;
    }
    if (out != null) {
      return out.committed();
    }
    return false;
  }

  @Override
  public void end() {
    if (ctx != null) {
      Attribute<NettyWebSocket> ws = ctx.attr(NettyWebSocket.KEY);
      if (ws != null && ws.get() != null) {
        status = HttpResponseStatus.SWITCHING_PROTOCOLS;
        ws.get().hankshake();
        ctx = null;
        return;
      }
      if (out == null) {
        DefaultFullHttpResponse rsp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        rsp.headers().set(headers);
        if (headers.contains(HttpHeaders.Names.CONTENT_LENGTH)) {
          if (keepAlive) {
            ctx.write(rsp);
          } else {
            ctx.write(rsp).addListener(ChannelFutureListener.CLOSE);
          }
        } else {
          ctx.write(rsp).addListener(ChannelFutureListener.CLOSE);
        }
      }
      ctx = null;
    }
  }

  @Override
  public void reset() {
    headers.clear();
    status = HttpResponseStatus.OK;
    if (out != null) {
      out.reset();
    }
  }

}
