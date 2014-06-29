package jooby.internal;

import java.util.Collections;
import java.util.Map;

public interface RouteMatcher {

  boolean matches();

  default Map<String, String> vars() {
    return Collections.emptyMap();
  }
}
