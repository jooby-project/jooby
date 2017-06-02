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

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.jooby.Sse;
import org.jooby.spi.NativePushPromise;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;
import org.jooby.spi.NativeWebSocket;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCounted;

public class NettyRequest implements NativeRequest {

  public static final AttributeKey<String> PROTOCOL = AttributeKey
      .newInstance(NettyRequest.class.getName() + ".protol");

  public static final AttributeKey<Boolean> NEED_FLUSH = AttributeKey
      .newInstance(NettyRequest.class.getName() + ".needFlush");

  public static final AttributeKey<Boolean> ASYNC = AttributeKey
      .newInstance(NettyRequest.class.getName() + ".async");

  public static final AttributeKey<Boolean> SECURE = AttributeKey
      .newInstance(NettyRequest.class.getName() + ".secure");;

  private HttpRequest req;

  private QueryStringDecoder query;

  private List<org.jooby.Cookie> cookies;

  private Multimap<String, String> params;

  private Multimap<String, NativeUpload> files;

  private String tmpdir;

  private String path;

  private ChannelHandlerContext ctx;

  private int wsMaxMessageSize;

  public NettyRequest(final ChannelHandlerContext ctx,
      final HttpRequest req, final String tmpdir,
      final int wsMaxMessageSize) throws IOException {
    this.ctx = ctx;
    this.req = req;
    this.tmpdir = tmpdir;
    this.query = new QueryStringDecoder(req.uri());
    this.path = URLDecoder.decode(query.path(), "UTF-8");
    this.wsMaxMessageSize = wsMaxMessageSize;
    Channel channel = ctx.channel();
    channel.attr(ASYNC).set(false);
  }

  @Override
  public Optional<String> queryString() {
    String uri = req.uri();
    int at = uri.indexOf('?') + 1;
    return at > 0 && at < uri.length() ? Optional.of(uri.substring(at)) : Optional.empty();
  }

  @Override
  public String method() {
    return req.method().name();
  }

  @Override
  public String rawPath() {
    String uri = req.uri();
    int at = uri.indexOf('?');
    return at > 0 ? uri.substring(0, at) : uri;
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
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    req.headers().names().forEach(it -> builder.add(it.toString()));
    return builder.build();
  }

  @Override
  public List<org.jooby.Cookie> cookies() {
    if (this.cookies == null) {
      String cookieString = req.headers().get(HttpHeaderNames.COOKIE);
      if (cookieString != null) {
        this.cookies = ServerCookieDecoder.STRICT.decode(cookieString).stream()
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
    return ImmutableList.copyOf(this.files.get(name));
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
  public String protocol() {
    return ctx.pipeline().get("h2") == null
        ? req.protocolVersion().text()
        : "HTTP/2.0";
  }

  @Override
  public boolean secure() {
    return ifSecure(Boolean.TRUE, Boolean.FALSE).booleanValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T upgrade(final Class<T> type) throws Exception {
    if (type == NativeWebSocket.class) {
      String protocol = ifSecure("wss", "ws");
      String webSocketURL = protocol + "://" + req.headers().get(HttpHeaderNames.HOST) + path;

      WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
          webSocketURL, null, true, wsMaxMessageSize);
      WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
      NettyWebSocket result = new NettyWebSocket(ctx, handshaker, (ws) -> {
        handshaker.handshake(ctx.channel(), (FullHttpRequest) req)
            .addListener(FIRE_EXCEPTION_ON_FAILURE)
            .addListener(payload -> ws.connect())
            .addListener(FIRE_EXCEPTION_ON_FAILURE);
      });
      ctx.channel().attr(NettyWebSocket.KEY).set(result);
      return (T) result;
    } else if (type == Sse.class) {
      NettySse sse = new NettySse(ctx);
      return (T) sse;
    } else if (type == NativePushPromise.class) {
      return (T) new NettyPush(ctx,
          req.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text()),
          header("host").orElse(ip()), ifSecure("https", "http"));
    }
    throw new UnsupportedOperationException("Not Supported: " + type);
  }

  @Override
  public void startAsync(final Executor executor, final Runnable runnable) {
    Channel channel = ctx.channel();
    channel.attr(NEED_FLUSH).set(false);
    channel.attr(ASYNC).set(true);

    ReferenceCounted body = ((ByteBufHolder) req).content();
    body.retain();
    executor.execute(() -> {
      try {
        runnable.run();
      } finally {
        body.release();
      }
    });
  }

  private org.jooby.Cookie cookie(final Cookie c) {
    org.jooby.Cookie.Definition cookie = new org.jooby.Cookie.Definition(c.name(), c.value());
    Optional.ofNullable(c.domain()).ifPresent(cookie::domain);
    Optional.ofNullable(c.path()).ifPresent(cookie::path);

    return cookie.toCookie();
  }

  private Multimap<String, String> decodeParams() throws IOException {
    if (params == null) {
      params = ArrayListMultimap.create();
      files = ArrayListMultimap.create();

      query.parameters()
          .forEach((name, values) -> values.forEach(value -> params.put(name, value)));

      HttpMethod method = req.method();
      boolean hasBody = method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)
          || method.equals(HttpMethod.PATCH);
      boolean formLike = false;
      if (req.headers().contains("Content-Type")) {
        String contentType = req.headers().get("Content-Type").toLowerCase();
        formLike = (contentType.startsWith(MediaType.multipart.name())
            || contentType.startsWith(MediaType.form.name()));
      }
      if (hasBody && formLike) {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
            new DefaultHttpDataFactory(), req);
        try {
          Function<HttpPostRequestDecoder, Boolean> hasNext = it -> {
            try {
              return it.hasNext();
            } catch (HttpPostRequestDecoder.EndOfDataDecoderException ex) {
              return false;
            }
          };
          while (hasNext.apply(decoder)) {
            HttpData field = (HttpData) decoder.next();
            try {
              String name = field.getName();
              if (field.getHttpDataType() == HttpDataType.FileUpload) {
                files.put(name, new NettyUpload((FileUpload) field, tmpdir));
              } else {
                params.put(name, field.getString());
              }
            } finally {
              field.release();
            }
          }
        } finally {
          decoder.destroy();
        }
      }
    }
    return params;
  }

  private <T> T ifSecure(final T then, final T otherwise) {
    return ctx.pipeline().get("ssl") != null ? then : otherwise;
  }
}
