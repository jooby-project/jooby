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
package org.jooby.internal.undertow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.jooby.Cookie;
import org.jooby.MediaType;
import org.jooby.Sse;
import org.jooby.spi.NativePushPromise;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;
import org.jooby.spi.NativeWebSocket;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.typesafe.config.Config;

import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

public class UndertowRequest implements NativeRequest {

  public static final AttachmentKey<NativeWebSocket> SOCKET = AttachmentKey
      .create(NativeWebSocket.class);

  private HttpServerExchange exchange;

  private Config config;

  private final FormData form;

  private final String path;

  private Supplier<BlockingHttpExchange> blocking;

  public UndertowRequest(final HttpServerExchange exchange, final Config conf)
      throws IOException {
    this.exchange = exchange;
    this.blocking = Suppliers.memoize(() -> this.exchange.startBlocking());
    this.config = conf;
    this.form = parseForm(exchange, conf.getString("application.tmpdir"),
        conf.getString("application.charset"));
    this.path = URLDecoder.decode(exchange.getRequestPath(), "UTF-8");
  }

  @Override
  public String method() {
    return exchange.getRequestMethod().toString();
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public List<String> paramNames() {
    ImmutableList.Builder<String> builder = ImmutableList.<String> builder();
    builder.addAll(exchange.getQueryParameters().keySet());
    form.forEach(v -> {
      // excludes upload from param names.
      if (!form.getFirst(v).isFile()) {
        builder.add(v);
      }
    });
    return builder.build();
  }

  @Override
  public List<String> params(final String name) {
    Builder<String> builder = ImmutableList.builder();
    // query params
    Deque<String> query = exchange.getQueryParameters().get(name);
    if (query != null) {
      query.stream().forEach(builder::add);
    }
    // form params
    Optional.ofNullable(form.get(name)).ifPresent(values -> {
      values.stream().forEach(value -> {
        if (!value.isFile()) {
          builder.add(value.getValue());
        }
      });
    });
    return builder.build();
  }

  @Override
  public Optional<String> header(final String name) {
    return Optional.ofNullable(exchange.getRequestHeaders().getFirst(name));
  }

  @Override
  public List<String> headers(final String name) {
    HeaderValues values = exchange.getRequestHeaders().get(name);
    return values == null ? Collections.emptyList() : values;
  }

  @Override
  public List<String> headerNames() {
    return exchange.getRequestHeaders().getHeaderNames()
        .stream()
        .map(HttpString::toString)
        .collect(Collectors.toList());
  }

  @Override
  public List<Cookie> cookies() {
    return exchange.getRequestCookies().values().stream()
        .map(UndertowRequest::cookie)
        .collect(Collectors.toList());
  }

  @Override
  public List<NativeUpload> files(final String name) {
    Builder<NativeUpload> builder = ImmutableList.builder();
    Deque<FormValue> values = form.get(name);
    if (values != null) {
      values.forEach(value -> {
        if (value.isFile()) {
          builder.add(new UndertowUpload(value));
        }
      });
    }
    return builder.build();
  }

  @Override
  public InputStream in() {
    blocking.get();
    return exchange.getInputStream();
  }

  @Override
  public String ip() {
    return Optional.ofNullable(exchange.getSourceAddress())
        .map(src -> Optional.ofNullable(src.getAddress())
            .map(InetAddress::getHostAddress)
            .orElse(""))
        .orElse("");
  }

  @Override
  public String protocol() {
    return exchange.getProtocol().toString();
  }

  @Override
  public boolean secure() {
    return exchange.getRequestScheme().equalsIgnoreCase("https");
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T upgrade(final Class<T> type) throws Exception {
    if (type == NativeWebSocket.class) {
      UndertowWebSocket ws = new UndertowWebSocket(config);
      exchange.putAttachment(SOCKET, ws);
      return (T) ws;
    }
    if (type == Sse.class) {
      return (T) new UndertowSse(exchange);
    }
    if (type == NativePushPromise.class) {
      return (T) new UndertowPush(exchange);
    }
    throw new UnsupportedOperationException("Not Supported: " + type);
  }

  @Override
  public void startAsync(final Executor executor, final Runnable runnable) {
    exchange.dispatch(executor, runnable);
  }

  private FormData parseForm(final HttpServerExchange exchange, final String tmpdir,
      final String charset) throws IOException {
    String value = exchange.getRequestHeaders().getFirst("Content-Type");
    if (value != null) {
      MediaType type = MediaType.valueOf(value);
      if (MediaType.form.name().equals(type.name())) {
        blocking.get();
        return new FormEncodedDataDefinition()
            .setDefaultEncoding(charset)
            .create(exchange)
            .parseBlocking();
      } else if (MediaType.multipart.name().equals(type.name())) {
        blocking.get();
        return new MultiPartParserDefinition()
            .setTempFileLocation(new File(tmpdir).toPath())
            .setDefaultEncoding(charset)
            .create(exchange)
            .parseBlocking();

      }
    }
    return new FormData(0);
  }

  private static Cookie cookie(final io.undertow.server.handlers.Cookie c) {
    Cookie.Definition cookie = new Cookie.Definition(c.getName(), c.getValue());
    Optional.ofNullable(c.getComment()).ifPresent(cookie::comment);
    Optional.ofNullable(c.getDomain()).ifPresent(cookie::domain);
    Optional.ofNullable(c.getPath()).ifPresent(cookie::path);

    return cookie.toCookie();
  }

}
