/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Router;

public class RouteTreeIgnoreTrailingSlash extends RouteTreeForwarding {
  public RouteTreeIgnoreTrailingSlash(RouteTree tree) {
    super(tree);
  }

  @Override public boolean exists(String method, String path) {
    return super.exists(method, Router.noTrailingSlash(path));
  }

  @Override public Router.Match find(String method, String path) {
    return super.find(method, Router.noTrailingSlash(path));
  }
}
