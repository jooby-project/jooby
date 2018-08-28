package io.jooby.internal;

import io.jooby.Route;
import io.jooby.Router;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouterMatch implements Router.Match {
  private boolean matches;

  private Route route;

  private Map vars = Collections.EMPTY_MAP;

  public void key(List<String> keys) {
    for (int i = 0; i < keys.size(); i++) {
      vars.put(keys.get(i), vars.remove(i));
    }
  }

  public void value(String value) {
    if (vars == Collections.EMPTY_MAP) {
      vars = new HashMap();
    }
    vars.put(vars.size(), value);
  }

  public void pop() {
    vars.remove(vars.size() - 1);
  }

  @Override public boolean matches() {
    return matches;
  }

  @Override public Route route() {
    return route;
  }

  @Override public Map<String, String> params() {
    return vars;
  }

  public RouterMatch result(RouteImpl route, boolean matches) {
    this.route = route;
    this.matches = matches;
    return this;
  }

}
