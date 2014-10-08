package jooby.internal.routes;

import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Route;
import jooby.Route.Definition;
import jooby.Router;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

public class OptionsRouter implements Router {

  private Set<Definition> routeDefs;

  @Inject
  public OptionsRouter(final Set<Route.Definition> routeDefs) {
    this.routeDefs = requireNonNull(routeDefs, "Route definitions are required.");
  }

  @Override
  public void handle(final Request req, final Response res) throws Exception {
    if (res.committed()) {
      return;
    }
    if (!res.header("Allow").toOptional(String.class).isPresent()) {
      Set<String> allow = new LinkedHashSet<>();
      Set<Request.Verb> verbs = EnumSet.allOf(Request.Verb.class);
      String path = req.path();
      verbs.remove(req.route().verb());
      for (Request.Verb alt : verbs) {
        for (Route.Definition routeDef : routeDefs) {
          Optional<Route> route = routeDef.matches(alt, path, MediaType.all, MediaType.ALL);
          if (route.isPresent()) {
            allow.add(route.get().verb().name());
          }
        }
      }
      res.header("Allow", Joiner.on(", ").join(allow));
      res.length(0);
      res.status(HttpStatus.OK);
    }
  }

}
