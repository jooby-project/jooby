/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.Context;
import io.jooby.exception.StatusCodeException;
import io.jooby.Router;
import io.jooby.StatusCode;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.nio.charset.StandardCharsets;

public class UtowHandler implements HttpHandler {
  protected final Router router;
  private final long maxRequestSize;
  private final int bufferSize;
  private final boolean defaultHeaders;

  public UtowHandler(Router router, int bufferSize, long maxRequestSize, boolean defaultHeaders) {
    this.router = router;
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
    UtowContext context = new UtowContext(exchange, router);
    Router.Match route = router.match(context);
    /** default headers: */
    HeaderMap responseHeaders = exchange.getResponseHeaders();
    responseHeaders.put(Headers.CONTENT_TYPE, "text/plain");
    if (defaultHeaders) {
      responseHeaders.put(Headers.SERVER, "U");
    }

    HeaderMap headers = exchange.getRequestHeaders();
    long len = parseLen(headers.getFirst(Headers.CONTENT_LENGTH));
    String chunked = headers.getFirst(Headers.TRANSFER_ENCODING);
    if (len > 0 || chunked != null) {
      if (len > maxRequestSize) {
        context.sendError(new StatusCodeException(StatusCode.REQUEST_ENTITY_TOO_LARGE));
        return;
      }
      /** Don't check/parse for body if there is no match: */
      if (!route.matches()) {
        route.execute(context);
        return;
      }
      /** Eager body parsing: */
      FormDataParser parser = FormParserFactory.builder(false)
          .addParser(new MultiPartParserDefinition(router.getTmpdir())
              .setDefaultEncoding(StandardCharsets.UTF_8.name()))
          .addParser(new FormEncodedDataDefinition()
              .setDefaultEncoding(StandardCharsets.UTF_8.name()))
          .build()
          .createParser(exchange);
      if (parser == null) {
        // Read raw body
        Receiver receiver = exchange.getRequestReceiver();
        UtowBodyHandler reader = new UtowBodyHandler(route, context, bufferSize, maxRequestSize);
        if (len > 0 && len <= bufferSize) {
          receiver.receiveFullBytes(reader);
        } else {
          receiver.receivePartialBytes(reader);
        }
      } else {
        try {
          parser.parse(execute(route, context));
        } catch (Exception x) {
          context.sendError(x, StatusCode.BAD_REQUEST);
        }
      }
    } else {
      route.execute(context);
    }
  }

  private static long parseLen(String value) {
    try {
      return value == null ? -1 : Long.parseLong(value);
    } catch (NumberFormatException x) {
      return -1;
    }
  }

  private static HttpHandler execute(Router.Match route, Context ctx) {
    return exchange -> route.execute(ctx);
  }
}
