package io.jooby.internal.jetty;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.undertow.util.Headers;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.eclipse.jetty.server.Request.__MULTIPART_CONFIG_ELEMENT;

public class JettyHandler extends AbstractHandler {
  private final Router router;

  private final Executor executor;
  private final Consumer<Request> multipart;

  public JettyHandler(Router router, Executor executor, Path tmpdir) {
    this.router = router;
    this.executor = executor;
    this.multipart = req ->
        req.setAttribute(__MULTIPART_CONFIG_ELEMENT,
            new MultipartConfigElement(tmpdir.toString(), -1L, -1L, Context._16KB))
    ;
  }

  @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
      HttpServletResponse response) throws IOException, ServletException {
    JettyContext context = new JettyContext(target, request, executor, multipart,
        router.errorHandler());
    Router.Match match = router.match(context);
    Route route = match.route();
    Route.RootHandler handler = route.pipeline();
    if (route.gzip() && acceptGzip(request.getHeader("Accept-Encoding"))) {
      /** Gzip: */
      GzipHandler jettyGzip = new GzipHandler();
      jettyGzip.setHandler(gzipCall(handler, context));
      jettyGzip.handle(target, request, request, request.getResponse());
    } else {
      handler.apply(context);
    }
  }

  private boolean acceptGzip(String value) {
    return value != null && value.contains("gzip");
  }

  private static Handler gzipCall(Route.RootHandler handler, JettyContext ctx) {
    return new AbstractHandler() {
      @Override public void handle(String target, Request baseRequest, HttpServletRequest request,
          HttpServletResponse response) {
        handler.apply(ctx);
      }
    };
  }
}
