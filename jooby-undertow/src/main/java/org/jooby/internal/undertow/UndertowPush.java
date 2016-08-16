package org.jooby.internal.undertow;

import java.util.Map;

import org.jooby.spi.NativePushPromise;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

public class UndertowPush implements NativePushPromise {

  private HttpServerExchange exchange;

  public UndertowPush(final HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void push(final String method, final String path, final Map<String, String> headers) {
    HeaderMap h2headers = new HeaderMap();
    headers.forEach((h, v) -> h2headers.put(HttpString.tryFromString(h), v));
    exchange.getConnection().pushResource(path, HttpString.tryFromString(method), h2headers);
  }

}
