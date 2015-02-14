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
package org.jooby.undertow.internal;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.MediaType;
import org.jooby.fn.Collectors;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeUpload;
import org.jooby.spi.NativeWebSocket;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class UndertowRequest implements NativeRequest {

  public static final AttachmentKey<NativeWebSocket> SOCKET = AttachmentKey
      .create(NativeWebSocket.class);

  private HttpServerExchange exchange;

  private final FormData form;

  public UndertowRequest(final HttpServerExchange exchange, final String tmpdir,
      final Charset charset) throws IOException {
    this.exchange = requireNonNull(exchange, "An undertow exchange is required.");
    this.form = parseForm(exchange, tmpdir, charset);
  }

  @Override
  public String method() {
    return exchange.getRequestMethod().toString();
  }

  @Override
  public String path() {
    return exchange.getRequestPath();
  }

  @Override
  public List<String> paramNames() {
    return ImmutableList.<String> builder()
        .addAll(exchange.getQueryParameters().keySet())
        .build();
  }

  @Override
  public List<String> params(final String name) {
    Builder<String> builder = ImmutableList.builder();
    // query params
    Deque<String> query = exchange.getQueryParameters().get(name);
    if (query != null) {
      query.forEach(builder::add);
    }
    // form params
    Optional.ofNullable(form.get(name)).ifPresent(values -> {
      values.forEach(value -> {
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
    return exchange.getInputStream();
  }

  @Override
  public String ip() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostAddress();
  }

  @Override
  public String hostname() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostName();
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
      Handlers.websocket((wsExchange, channel) -> {
        exchange.putAttachment(SOCKET, new UndertowWebSocket(channel));
      }).handleRequest(exchange);

      T ws = (T) exchange.getAttachment(SOCKET);
      checkState(ws != null, "Upgrade didn't success");
      return ws;
    }
    throw new UnsupportedOperationException("Not Supported: " + type);
  }

  private FormData parseForm(final HttpServerExchange exchange, final String tmpdir,
      final Charset charset) throws IOException {
    String value = exchange.getRequestHeaders().getFirst("Content-Type");
    if (value != null) {
      MediaType type = MediaType.valueOf(value);
      if (MediaType.form.name().equals(type.name())) {
        return new FormEncodedDataDefinition()
            .setDefaultEncoding(charset.name())
            .create(exchange)
            .parseBlocking();
      } else if (MediaType.multipart.name().equals(type.name())) {
        return new MultiPartParserDefinition()
            .setTempFileLocation(new File(tmpdir))
            .setDefaultEncoding(charset.name())
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
    Optional.ofNullable(c.getMaxAge()).ifPresent(cookie::maxAge);
    cookie.httpOnly(c.isHttpOnly());
    cookie.secure(c.isSecure());

    return cookie.toCookie();
  }

}
