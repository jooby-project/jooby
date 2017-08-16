package org.jooby.internal.apitool;

import org.jooby.Route;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.RouteMethod;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class APIProvider implements Provider<List<RouteMethod>> {

  private final List<RouteMethod> routes;

  @Inject
  public APIProvider(ApiParser parser,
      @Named("application.class") String application, Set<Route.Definition> routes)
      throws Exception {
    this.routes = parser.parse(application, new ArrayList<>(routes));
  }

  @Override public List<RouteMethod> get() {
    return routes;
  }
}
