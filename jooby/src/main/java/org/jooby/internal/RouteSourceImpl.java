package org.jooby.internal;

import java.util.Optional;

import org.jooby.Route;

public class RouteSourceImpl implements Route.Source {

  private Optional<String> declaringClass;

  private int line;

  public RouteSourceImpl(final String declaringClass, final int line) {
    this.declaringClass = Optional.ofNullable(declaringClass);
    this.line = line;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public Optional<String> declaringClass() {
    return declaringClass;
  }

  @Override
  public String toString() {
    return declaringClass.orElse("~unknown") + ":" + line;
  }
}
