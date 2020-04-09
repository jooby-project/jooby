/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

public class RouteTreeForwarding implements RouteTree {
  private RouteTree tree;

  public RouteTreeForwarding(RouteTree tree) {
    this.tree = tree;
  }

  @Override public void insert(String method, String pattern, Route route) {
    tree.insert(method, pattern, route);
  }

  @Override public boolean exists(String method, String path) {
    return tree.exists(method, path);
  }

  @Override public Router.Match find(String method, String path) {
    return tree.find(method, path);
  }

  @Override public void destroy() {
    tree.destroy();
  }
}
