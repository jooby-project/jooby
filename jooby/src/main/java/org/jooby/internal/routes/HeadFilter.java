package org.jooby.internal.routes;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import org.jooby.Filter;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.RouteImpl;

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
