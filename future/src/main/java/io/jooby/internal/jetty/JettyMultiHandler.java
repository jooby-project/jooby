package io.jooby.internal.jetty;

import io.jooby.App;
import io.jooby.Router;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class JettyMultiHandler extends JettyHandler {
  private final List<App> routers;

  public JettyMultiHandler(Router router, List<App> routers) {
    super(router);
    this.routers = routers;
  }

  @Override public void handle(String target, Request request, HttpServletRequest servletRequest,
      HttpServletResponse response) throws IOException, ServletException {
    for (Router router : routers) {
      JettyContext ctx = new JettyContext(request, router.errorHandler(), router.tmpdir());
      Router.Match match = router.match(ctx);
      if (match.matches()) {
        handleMatch(target, request, response, ctx, router, match.route());
        return;
      }
    }
  }
}
