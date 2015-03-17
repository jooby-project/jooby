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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;
import org.jooby.util.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class NettyRequest implements NativeRequest {

  private HttpRequest req;

  private QueryStringDecoder query;

  private List<org.jooby.Cookie> cookies;

  private Multimap<String, String> params;

  private Multimap<String, NativeUpload> files;

  private String tmpdir;

  private String path;

  private ChannelHandlerContext ctx;

  public NettyRequest(final ChannelHandlerContext ctx, final HttpRequest req, final String tmpdir)
      throws IOException {
    this.ctx = ctx;
    this.req = req;
    this.tmpdir = tmpdir;
    this.query = new QueryStringDecoder(req.getUri());
    this.path = URLDecoder.decode(query.path(), "UTF-8");
  }

  @Override
  public String method() {
    return req.getMethod().name();
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public List<String> paramNames() throws IOException {
    return ImmutableList.copyOf(decodeParams().keySet());
  }

  @Override
  public List<String> params(final String name) throws Exception {
    return (List<String>) decodeParams().get(name);
  }

  @Override
  public List<String> headers(final String name) {
    return req.headers().getAll(name);
  }

  @Override
  public Optional<String> header(final String name) {
    String value = req.headers().get(name);
    return Optional.ofNullable(value);
  }

  @Override
  public List<String> headerNames() {
    return ImmutableList.copyOf(req.headers().names());
  }

  @Override
  public List<org.jooby.Cookie> cookies() {
    if (this.cookies == null) {
      String cookieString = req.headers().get(HttpHeaders.Names.COOKIE);
      if (cookieString != null) {
        this.cookies = CookieDecoder.decode(cookieString).stream()
            .map(this::cookie)
            .collect(Collectors.toList());

      } else {
        this.cookies = Collections.emptyList();
      }
    }
    return this.cookies;
  }

  @Override
  public List<NativeUpload> files(final String name) throws IOException {
    decodeParams();
    if (files.size() == 0) {
      return Collections.emptyList();
    }
    Collection<NativeUpload> files = this.files.get(name);
    return files == null ? Collections.emptyList() : ImmutableList.copyOf(files);
  }

  @Override
  public InputStream in() throws IOException {
    ByteBuf content = ((HttpContent) req).content();
    return new ByteBufInputStream(content);
  }

  @Override
  public String ip() {
    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    return remoteAddress.getAddress().getHostAddress();
  }

  @Override
  public String hostname() {
    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    return remoteAddress.getAddress().getHostName();
  }

  @Override
  public String protocol() {
    return req.getProtocolVersion().text();
  }

  @Override
  public boolean secure() {
    return protocol().toLowerCase().startsWith("https");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T upgrade(final Class<T> type) throws Exception {
    String protocol = secure() ? "wss" : "ws";
    String webSocketURL = protocol + "://" + req.headers().get(HttpHeaders.Names.HOST) + path;

    WebSocketServerHandshakerFactory wsFactory =
        new WebSocketServerHandshakerFactory(webSocketURL, null, true);
    WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
    NettyWebSocket result = new NettyWebSocket(ctx, handshaker, (ws) -> {
      handshaker.handshake(ctx.channel(), (FullHttpRequest) req)
          .addListener(payload -> ws.connect());
    });
    ctx.attr(NettyWebSocket.KEY).set(result);
    return (T) result;
  }

  private org.jooby.Cookie cookie(final Cookie c) {
    org.jooby.Cookie.Definition cookie = new org.jooby.Cookie.Definition(c.getName(), c.getValue());
    Optional.ofNullable(c.getComment()).ifPresent(cookie::comment);
    Optional.ofNullable(c.getDomain()).ifPresent(cookie::domain);
    Optional.ofNullable(c.getPath()).ifPresent(cookie::path);
    Optional.ofNullable(c.getMaxAge()).ifPresent(maxAge -> {
      if (maxAge >= 0) {
        cookie.maxAge(0);
      }
    });
    cookie.httpOnly(c.isHttpOnly());
    cookie.secure(c.isSecure());

    return cookie.toCookie();
  }

  private Multimap<String, String> decodeParams() throws IOException {
    if (params == null) {
      params = ArrayListMultimap.create();
      files = ArrayListMultimap.create();

      query.parameters()
          .forEach((name, values) -> values.forEach(value -> params.put(name, value)));

      HttpMethod method = req.getMethod();
      if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)
          || method.equals(HttpMethod.PATCH)) {
        HttpPostRequestDecoder form = new HttpPostRequestDecoder(req);
        Function<HttpPostRequestDecoder, Boolean> hasNext = it -> {
          try {
            return it.hasNext();
          } catch (HttpPostRequestDecoder.EndOfDataDecoderException ex) {
            return false;
          }
        };
        while (hasNext.apply(form)) {
          HttpData field = (HttpData) form.next();
          String name = field.getName();
          switch (field.getHttpDataType()) {
            case FileUpload:
              files.put(name, new NettyUpload((FileUpload) field, tmpdir));
            default:
              params.put(name, field.getString());
              break;
          }
        }
      }
    }
    return params;
  }
}
