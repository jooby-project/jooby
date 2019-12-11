package io.jooby.internal;

import io.jooby.MessageEncoder;
import io.jooby.Router;

public class RouteTreeWithOptions extends RouteTreeForwarding {
  private final boolean lowerCase;
  private final boolean noTrailingSlash;

  public RouteTreeWithOptions(RouteTree tree, boolean lowerCase, boolean noTrailingSlash) {
    super(tree);
    this.lowerCase = lowerCase;
    this.noTrailingSlash = noTrailingSlash;
  }

  @Override public boolean find(String method, String path) {
    return super.find(method, path(path));
  }

  private String path(String path) {
    if (lowerCase) {
      path = path.toLowerCase();
    }
    if (noTrailingSlash) {
      path = Router.noTrailingSlash(path);
    }
    return path;
  }

  @Override public RouterMatch find(String method, String path, MessageEncoder encoder) {
    return super.find(method, path(path), encoder);
  }
}
