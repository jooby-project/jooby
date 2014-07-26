package jooby;

import java.util.Collections;
import java.util.Map;

public interface RouteMatcher {

  String path();

  boolean matches();

  default Map<String, String> vars() {
    return Collections.emptyMap();
  }
}
