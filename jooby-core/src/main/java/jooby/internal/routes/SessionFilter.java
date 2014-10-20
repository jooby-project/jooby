package jooby.internal.routes;

import static java.util.Objects.requireNonNull;
import jooby.Cookie;
import jooby.Filter;
import jooby.Request;
import jooby.Response;
import jooby.Route.Chain;

public class SessionFilter implements Filter {

  private Cookie cookie;

  public SessionFilter(final Cookie cookie) {
    this.cookie = requireNonNull(cookie, "A cookie is required.");
  }

  @Override
  public void handle(final Request req, final Response res, final Chain chain) throws Exception {
    if (req.session() != null) {
      chain.next(req, res);
      return;
    }

    // path mismatch
    if (!req.path().startsWith(cookie.path())) {
      chain.next(req, res);
      return;
    }
  }

}
