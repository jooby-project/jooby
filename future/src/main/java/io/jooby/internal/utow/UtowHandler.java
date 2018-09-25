package io.jooby.internal.utow;

import io.jooby.Route;
import io.jooby.Router;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.nio.file.Path;

public class UtowHandler implements HttpHandler {

  private static final String UTOW = "Utow";
  private final Router router;
  private final Path tmpdir;

  public UtowHandler(Router router, Path tmpdir) {
    this.router = router;
    this.tmpdir = tmpdir;
  }

  @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
    UtowContext context = new UtowContext(exchange, router.errorHandler(), tmpdir);
    Router.Match match = router.match(context);
    Route route = match.route();
    Route.RootHandler handler = route.pipeline();

    if (route.gzip() && acceptGzip(exchange.getRequestHeaders().getFirst(Headers.ACCEPT_ENCODING))) {
      new EncodingHandler.Builder().build(null)
          .wrap(gzipExchange -> handler.apply(context))
          .handleRequest(exchange);
    } else {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, router.defaultContentType());
      handler.apply(context);
    }
  }

  private boolean acceptGzip(String value) {
    return value != null && value.contains("gzip");
  }
}
