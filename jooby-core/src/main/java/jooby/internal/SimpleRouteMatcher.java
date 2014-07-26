package jooby.internal;

import static java.util.Objects.requireNonNull;
import jooby.RouteMatcher;

class SimpleRouteMatcher implements RouteMatcher {

  private final String pattern;

  private final String fullpath;

  private final String path;

  public SimpleRouteMatcher(final String path, final String pattern, final String fullpath) {
    this.path = requireNonNull(path, "The path is required.");
    this.pattern = requireNonNull(pattern, "The pattern is required.");
    this.fullpath = requireNonNull(fullpath, "The full path is required.");
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public boolean matches() {
    return fullpath.equals(pattern);
  }

  @Override
  public String toString() {
    return pattern;
  }
}
