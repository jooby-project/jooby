package org.jooby.internal.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

import org.jooby.internal.RouteHandler;

public class UndertowHandler implements HttpHandler {

  private RouteHandler handler;

  public UndertowHandler(final RouteHandler handler) {
    this.handler = handler;
  }

  @Override
  public void handleRequest(final HttpServerExchange exchange) throws Exception {
    HeaderMap headers = exchange.getRequestHeaders();

    handler.handle(exchange, exchange.getRequestMethod().toString(),
        exchange.getRequestURI(), (name) -> {
          HeaderValues values = headers.get(name);
          return values == null ? null : values.getFirst();
        });
  }

}
