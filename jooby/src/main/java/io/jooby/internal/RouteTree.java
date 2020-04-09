/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Route;
import io.jooby.Router;

interface RouteTree {
  void insert(String method, String pattern, Route route);

  boolean exists(String method, String path);

  Router.Match find(String method, String path);

  void destroy();
}
