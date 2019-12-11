/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Route;

interface RouteTree {
  void insert(String method, String pattern, Route route);

  boolean find(String method, String path);

  RouterMatch find(String method, String path, MessageEncoder encoder);

  void destroy();
}
