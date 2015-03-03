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
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.Attribute;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooby.spi.NativeResponse;

import com.google.common.collect.ImmutableList;

public class NettyResponse implements NativeResponse {

  private ChannelHandlerContext ctx;

  private NettyRequest req;

  private boolean keepAlive;

  HttpResponseStatus status = HttpResponseStatus.OK;

  private Map<String, Cookie> cookies = new HashMap<>();

  HttpHeaders headers;

  private NettyOutputStream out;

  public NettyResponse(final ChannelHandlerContext ctx, final NettyRequest req,
      final boolean keepAlive) {
    this.ctx = ctx;
    this.req = req;
    this.keepAlive = keepAlive;
    this.headers = new DefaultHttpHeaders();
  }

  @Override
  public void cookie(final org.jooby.Cookie cookie) {
    cookies.put(cookie.name(), toCookie(cookie));
  }

  @Override
  public void clearCookie(final String name) {
    cookies.remove(name);
    req.cookies().stream().filter(c -> c.name().equals(name)).findFirst().ifPresent(c -> {
      Cookie cookie = toCookie(c);
      cookie.setMaxAge(0);
      cookies.put(name, cookie);
    });

  }

  @Override
  public List<String> headers(final String name) {
    List<String> headers = this.headers.getAll(name);
    return headers == null ? Collections.emptyList() : ImmutableList.copyOf(headers);
  }

  @Override
  public void header(final String name, final String value) {
    headers.set(name, value);
  }

  @Override
  public OutputStream out(final int bufferSize) throws IOException {
    if (out == null) {
      if (cookies.size() > 0) {
        headers.set(HttpHeaders.Names.SET_COOKIE,
            ServerCookieEncoder.encode(cookies.values()));
      }
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
    return ctx == null;
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
        if (cookies.size() > 0) {
          headers.set(HttpHeaders.Names.SET_COOKIE,
              ServerCookieEncoder.encode(cookies.values()));
        }
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

  private Cookie toCookie(final org.jooby.Cookie cookie) {
    Cookie result =
        new DefaultCookie(cookie.name(), cookie.value().orElse(null));

    cookie.comment().ifPresent(result::setComment);
    cookie.domain().ifPresent(result::setDomain);
    result.setHttpOnly(cookie.httpOnly());
    long maxAge = cookie.maxAge();
    if (maxAge >= 0) {
      result.setMaxAge(maxAge);
    } else {
      result.setMaxAge(Long.MIN_VALUE);
    }
    result.setPath(cookie.path());
    result.setSecure(cookie.secure());
    result.setVersion(cookie.version());

    return result;
  }

}
