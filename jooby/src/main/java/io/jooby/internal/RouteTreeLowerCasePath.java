/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.MessageEncoder;

public class RouteTreeLowerCasePath extends RouteTreeForwarding {
  public RouteTreeLowerCasePath(RouteTree tree) {
    super(tree);
  }

  @Override public boolean find(String method, String path) {
    return super.find(method, path.toLowerCase());
  }

  @Override public RouterMatch find(String method, String path, MessageEncoder encoder) {
    return super.find(method, path.toLowerCase(), encoder);
  }
}
