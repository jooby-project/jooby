package io.jooby.internal.jetty;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
      HttpServletResponse response) {
    JettyContext context = new JettyContext(target, request, executor, multipart,
        router.errorHandler());
    Router.Match match = router.match(context);
    Route.RootHandler handler = match.route().pipeline();
    handler.apply(context);
  }
}
