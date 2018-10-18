package io.jooby.internal.utow;

import io.jooby.App;
import io.jooby.Router;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class UtowMultiHandler implements HttpHandler {
  private final Map<App, UtowHandler> router;

  public UtowMultiHandler(Map<App, UtowHandler> router) {
    this.router = router;
  }

  @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
    for (Map.Entry<App, UtowHandler> e : router.entrySet()) {
      App r = e.getKey();
      UtowContext context = new UtowContext(exchange, r.errorHandler(), r.tmpdir());
      Router.Match match = r.match(context);
      if (match.matches()) {
        e.getValue().handle(exchange, context, r, match.route());
      }
    }
  }
}
