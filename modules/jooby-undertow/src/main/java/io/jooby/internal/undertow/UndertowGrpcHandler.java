/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import java.util.concurrent.Flow;
import java.util.function.Function;

import io.jooby.Context;
import io.jooby.Router;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class UndertowGrpcHandler implements HttpHandler {
  private final HttpHandler next;
  private final Router router;
  private final int bufferSize;
  private final Function<Context, Flow.Subscriber<byte[]>> subscriberFactory;

  public UndertowGrpcHandler(
      HttpHandler next,
      Router router,
      int bufferSize,
      Function<Context, Flow.Subscriber<byte[]>> subscriberFactory) {
    this.next = next;
    this.router = router;
    this.bufferSize = bufferSize;
    this.subscriberFactory = subscriberFactory;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (!exchange
        .getRequestHeaders()
        .get(Headers.CONTENT_TYPE)
        .getFirst()
        .contains("application/grpc")) {
      next.handleRequest(exchange);
    } else {
      // Prevents Undertow from automatically closing/draining the request
      //      exchange.setPersistent(true);

      // 2. IMPORTANT: Dispatch to a worker thread so we don't block the IO thread
      exchange.dispatch(
          () -> {
            // Ensure we don't trigger the default draining behavior
            var context = new UndertowContext(exchange, router, bufferSize);
            var subscriber = subscriberFactory.apply(context);
            new UndertowRequestPublisher(exchange).subscribe(subscriber);
          });
    }
  }
}
