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

import static java.util.Objects.requireNonNull;
import io.undertow.Handlers;
import io.undertow.io.UndertowOutputStream;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.spi.NativeResponse;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndertowResponse implements NativeResponse {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(NativeResponse.class);

  private HttpServerExchange exchange;

  private OutputStream stream;

  public UndertowResponse(final HttpServerExchange exchange)
      throws IOException {
    this.exchange = requireNonNull(exchange, "An undertow exchange is required.");
  }

  @Override
  public void cookie(final Cookie cookie) {
    requireNonNull(cookie, "A cookie is required.");

    exchange.getResponseCookies().put(cookie.name(), toUndertowCookie(cookie));
  }

  @Override
  public void clearCookie(final String name) {
    requireNonNull(name, "A cookie's name is required.");
    // it was set in the current req/rsp call?
    exchange.getResponseCookies().remove(name);

    // or clear existing cookie
    Optional.ofNullable(exchange.getRequestCookies().get(name)).ifPresent(cookie -> {
      cookie.setMaxAge(0);
      exchange.getResponseCookies().put(name, cookie);
    });
  }

  @Override
  public List<String> headers(final String name) {
    requireNonNull(name, "A header's name is required.");
    HeaderValues values = exchange.getResponseHeaders().get(name);
    return values == null ? Collections.emptyList() : values;
  }

  @Override
  public void header(final String name, final String value) {
    exchange.getResponseHeaders().put(new HttpString(name), value);
  }

  @Override
  public OutputStream out(final int bufferSize) {
    if (stream == null) {
      stream = exchange.getOutputStream();
    }
    return stream;
  }

  @Override
  public int statusCode() {
    return exchange.getResponseCode();
  }

  @Override
  public void statusCode(final int code) {
    exchange.setResponseCode(code);
  }

  @Override
  public boolean committed() {
    return exchange.isResponseStarted();
  }

  @Override
  public void reset() {
    if (stream != null) {
      ((UndertowOutputStream) stream).resetBuffer();
    }
    exchange.getResponseHeaders().clear();
  }

  @Override
  public void end() {
    NativeWebSocket ws = exchange.getAttachment(UndertowRequest.SOCKET);
    if (ws != null) {
      try {
        Handlers.websocket((wsExchange, channel) -> {
          ((UndertowWebSocket) ws).connect(channel);
        }).handleRequest(exchange);
      } catch (Exception ex) {
        log.error("Upgrade result in exception", ex);
      } finally {
        exchange.removeAttachment(UndertowRequest.SOCKET);
      }
    }
    // this is a noop when response has been set, still call it...
    exchange.endExchange();
  }

  private io.undertow.server.handlers.Cookie toUndertowCookie(final Cookie cookie) {
    io.undertow.server.handlers.Cookie result =
        new CookieImpl(cookie.name(), cookie.value().orElse(null));

    cookie.comment().ifPresent(result::setComment);
    cookie.domain().ifPresent(result::setDomain);
    result.setHttpOnly(cookie.httpOnly());
    result.setMaxAge((int) cookie.maxAge());
    result.setPath(cookie.path());
    result.setSecure(cookie.secure());
    result.setVersion(cookie.version());

    return result;
  }
}
