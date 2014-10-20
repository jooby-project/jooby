package org.jooby.internal;

import java.util.Collections;
import java.util.Map;

import org.jooby.Route;

public interface RouteMatcher {

  /**
   * @return Current path under test.
   */
  String path();

  /**
   * @return True, if {@link #path()} matches a {@link Route.Pattern}.
   */
  boolean matches();

  /**
   * Get path vars from current path. Or empty map if there is none.
   * This method must be invoked after {@link #matches()}.
   *
   * @return Get path vars from current path. Or empty map if there is none.
   */
  default Map<String, String> vars() {
    return Collections.emptyMap();
  }
}
