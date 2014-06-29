package jooby.internal;

import static java.util.Objects.requireNonNull;

class SimpleRouteMatcher implements RouteMatcher {

  private String pattern;

  private String path;

  public SimpleRouteMatcher(final String pattern, final String path) {
    this.pattern = requireNonNull(pattern, "The pattern is required.");
    this.path = requireNonNull(path, "The path is required.");
  }

  @Override
  public boolean matches() {
    return path.equals(pattern);
  }

  @Override
  public String toString() {
    return pattern;
  }
}
