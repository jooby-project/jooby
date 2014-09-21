package jooby.internal;

import static java.util.Objects.requireNonNull;
import jooby.RouteMatcher;
import jooby.RoutePattern;

class SimpleRouteMatcher implements RouteMatcher {

  private final RoutePattern pattern;

  private final String fullpath;

  private final String path;

  public SimpleRouteMatcher(final RoutePattern pattern, final String path, final String fullpath) {
    this.pattern = requireNonNull(pattern, "The pattern is required.");
    this.path = requireNonNull(path, "The path is required.");
    this.fullpath = requireNonNull(fullpath, "The full path is required.");
  }

  @Override
  public RoutePattern pattern() {
    return pattern;
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public boolean matches() {
    return fullpath.equals(pattern.pattern());
  }

  @Override
  public String toString() {
    return pattern.toString();
  }
}
