package jooby;

import java.util.Collections;
import java.util.Map;

import com.google.common.annotations.Beta;

/**
 * A result of {@link RoutePattern#matcher(String)} which provides access to path variables.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface RouteMatcher {

  /**
   * @return Current path under test.
   */
  String path();

  /**
   * @return True, if {@link #path()} matches a {@link RoutePattern}.
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
