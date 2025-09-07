/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.nio.charset.StandardCharsets;

import io.jooby.*;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class UndertowHandler implements HttpHandler {
  private final long maxRequestSize;
  private final int bufferSize;
  private final boolean defaultHeaders;
  private final Context.Selector ctxSelector;

  public UndertowHandler(
      Context.Selector contextSelector,
      int bufferSize,
      long maxRequestSize,
      boolean defaultHeaders) {
    this.ctxSelector = contextSelector;
    this.maxRequestSize = maxRequestSize;
    this.bufferSize = bufferSize;
    this.defaultHeaders = defaultHeaders;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var router = ctxSelector.select(exchange.getRequestPath());
    var context = new UndertowContext(exchange, router);

    /* default headers: */
    HeaderMap responseHeaders = exchange.getResponseHeaders();
    responseHeaders.put(Headers.CONTENT_TYPE, "text/plain;charset=utf-8");
    if (defaultHeaders) {
      responseHeaders.put(Headers.SERVER, "U");
    }

    if (context.isHttpGet()) {
      router.match(context).execute(context);
    } else {
      // possibly  HTTP body
      HeaderMap headers = exchange.getRequestHeaders();
      long len = parseLen(headers.getFirst(Headers.CONTENT_LENGTH));
      String chunked = headers.getFirst(Headers.TRANSFER_ENCODING);
      if (len > 0 || chunked != null) {
        if (len > maxRequestSize) {
          Router.Match route = router.match(context);
          if (route.matches()) {
            route.execute(context, Route.REQUEST_ENTITY_TOO_LARGE);
          } else {
            // 404
            route.execute(context);
          }
          return;
        }

        /* Eager body parsing: */
        FormDataParser parser =
            FormParserFactory.builder(false)
                .addParser(
                    new MultiPartParserDefinition(router.getTmpdir())
                        .setDefaultEncoding(StandardCharsets.UTF_8.name()))
                .addParser(
                    new FormEncodedDataDefinition()
                        .setDefaultEncoding(StandardCharsets.UTF_8.name()))
                .build()
                .createParser(exchange);
        if (parser == null) {
          // Read raw body
          Receiver receiver = exchange.getRequestReceiver();
          Router.Match route = router.match(context);
          UndertowBodyHandler reader =
              new UndertowBodyHandler(route, context, bufferSize, maxRequestSize);
          if (len > 0 && len <= bufferSize) {
            receiver.receiveFullBytes(reader);
          } else {
            receiver.receivePartialBytes(reader);
          }
        } else {
          try {
            parser.parse(execute(router, context));
          } catch (Exception x) {
            context.sendError(x, StatusCode.BAD_REQUEST);
          }
        }
      } else {
        // no body move one:
        router.match(context).execute(context);
      }
    }
  }

  private static long parseLen(String value) {
    try {
      return value == null ? -1 : Long.parseLong(value);
    } catch (NumberFormatException x) {
      return -1;
    }
  }

  private static HttpHandler execute(Router router, Context ctx) {
    return exchange -> router.match(ctx).execute(ctx);
  }
}
