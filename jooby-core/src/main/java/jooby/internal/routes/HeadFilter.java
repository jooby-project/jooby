package jooby.internal.routes;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import jooby.Filter;
import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Route;
import jooby.Route.Definition;
import jooby.internal.RouteImpl;

import com.google.inject.Inject;

public class HeadFilter implements Filter {

  private Set<Definition> routeDefs;

  @Inject
  public HeadFilter(final Set<Route.Definition> routeDefs) {
    this.routeDefs = requireNonNull(routeDefs, "Route definitions are required.");
  }

  @Override
  public void handle(final Request req, final Response res, final Route.Chain chain)
      throws Exception {
    if (res.committed()) {
      return;
    }

    String path = req.path();
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef
          .matches(Request.Verb.GET, path, MediaType.all, MediaType.ALL);
      if (route.isPresent() && !route.get().pattern().contains("*")) {
        // route found
        res.length(0);
        ((RouteImpl) route.get()).handle(req, res, chain);
        return;
      }
    }
    // not handle, just call next
    chain.next(req, res);
  }

}
