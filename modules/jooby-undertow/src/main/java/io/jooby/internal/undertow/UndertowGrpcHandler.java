/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import io.jooby.rpc.grpc.GrpcProcessor;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Protocols;

public class UndertowGrpcHandler implements HttpHandler {

  private final HttpHandler next;
  private final GrpcProcessor processor;

  public UndertowGrpcHandler(HttpHandler next, GrpcProcessor processor) {
    this.next = next;
    this.processor = processor;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    var contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);

    if (processor.isGrpcMethod(exchange.getRequestPath())
        && contentType != null
        && contentType.startsWith("application/grpc")) {

      // gRPC strictly requires HTTP/2
      if (!exchange.getProtocol().equals(Protocols.HTTP_2_0)) {
        exchange.setStatusCode(426); // Upgrade Required
        exchange.getResponseHeaders().put(Headers.CONNECTION, "Upgrade");
        exchange.getResponseHeaders().put(Headers.UPGRADE, "h2c");
        exchange.endExchange();
        return;
      }

      var grpcExchange = new UndertowGrpcExchange(exchange);
      var subscriber = processor.process(grpcExchange);

      // Starts the reactive pipeline and acquires the XNIO channel
      var inputBridge = new UndertowGrpcInputBridge(exchange, subscriber);
      inputBridge.start();

      return; // Fully handled, do not pass to the standard router
    }

    // Not a gRPC request, delegate to the next handler in the chain
    next.handleRequest(exchange);
  }
}
