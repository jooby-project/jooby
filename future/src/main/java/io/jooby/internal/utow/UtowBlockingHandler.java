package io.jooby.internal.utow;

import io.jooby.Router;
import io.undertow.server.HttpServerExchange;

public class UtowBlockingHandler extends UtowHandler {
  public UtowBlockingHandler(Router router) {
    super(router);
  }

  @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
    exchange.startBlocking();
    if (exchange.isInIoThread()) {
      exchange.dispatch(router.executor("worker"), this);
    } else {
      super.handleRequest(exchange);
    }
  }
}
