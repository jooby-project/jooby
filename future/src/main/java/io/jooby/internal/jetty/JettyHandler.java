package io.jooby.internal.jetty;

import io.jooby.Route;
import io.jooby.Router;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Executor;

public class JettyHandler extends AbstractHandler {
  private final Router router;

  private final Executor executor;

  public JettyHandler(Router router, Executor executor) {
    this.router = router;
    this.executor = executor;
  }

  @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
      HttpServletResponse response) {
    String path = request.getRequestURI();
    Route route = router.match(request.getMethod().toUpperCase(), path);
    Route.RootHandler handler = router.asRootHandler(route.pipeline());
    handler.apply(new JettyContext(target, request, executor, route));
  }
}
