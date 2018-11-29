package io.jooby.internal.utow;

import io.jooby.Route;
import io.jooby.Router;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.util.Headers;

import java.util.concurrent.Executor;

public class UtowHandler implements HttpHandler {

  protected final Router router;

  public UtowHandler(Router router) {
    this.router = router;
  }

  @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
    UtowContext context = newContext(exchange, router);
    Router.Match match = router.match(context);
    Route route = match.route();
    handle(exchange, context, router, route);
  }

  protected UtowContext newContext(HttpServerExchange exchange, Router router) {
    return new UtowContext(exchange, router.errorHandler(), router.tmpdir());
  }

  public void handle(HttpServerExchange exchange, UtowContext context, Router router, Route route)
      throws Exception {
    Route.Handler handler = route.pipeline();

    if (route.gzip() && acceptGzip(
        exchange.getRequestHeaders().getFirst(Headers.ACCEPT_ENCODING))) {
      new EncodingHandler.Builder().build(null)
          .wrap(gzipExchange -> handler.execute(context))
          .handleRequest(exchange);
    } else {
      handler.execute(context);
    }
  }

  private boolean acceptGzip(String value) {
    return value != null && value.contains("gzip");
  }
}
