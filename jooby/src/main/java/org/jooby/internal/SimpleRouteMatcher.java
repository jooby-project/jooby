package org.jooby.internal;

import static java.util.Objects.requireNonNull;

class SimpleRouteMatcher implements RouteMatcher {

  private final String fullpath;

  private final String path;

  private String pattern;

  public SimpleRouteMatcher(final String pattern, final String path, final String fullpath) {
    this.pattern = requireNonNull(pattern, "A pattern is required.");
    this.path = requireNonNull(path, "A path is required.");
    this.fullpath = requireNonNull(fullpath, "A full path is required.");
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public boolean matches() {
    return fullpath.equals(pattern);
  }

}
