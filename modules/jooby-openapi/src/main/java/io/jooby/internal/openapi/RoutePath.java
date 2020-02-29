/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.Router;

public class RoutePath {
  public static String path(String prefix, String path) {
    String s1 = Router.leadingSlash(prefix);
    String s2 = Router.leadingSlash(path);
    if (s1.equals("/")) {
      return s2;
    }
    return s2.equals("/") ? s1 : s1 + s2;
  }
}
