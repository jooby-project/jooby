package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Router;

public class RouteTreeIgnoreTrailingSlash extends RouteTreeForwarding {
  public RouteTreeIgnoreTrailingSlash(RouteTree tree) {
    super(tree);
  }

  @Override public boolean find(String method, String path) {
    return super.find(method, Router.noTrailingSlash(path));
  }

  @Override public RouterMatch find(String method, String path, MessageEncoder encoder) {
    return super.find(method, Router.noTrailingSlash(path), encoder);
  }
}
