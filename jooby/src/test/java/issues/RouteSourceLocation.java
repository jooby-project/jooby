package issues;

import org.jooby.Route;

import java.util.function.Function;

public class RouteSourceLocation {
  public Function<String, Route.Definition> route() {
    return path -> new Route.Definition("*", path, () -> null);
  }
}
