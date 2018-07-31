package io.jooby;

public class Route {

  private final String method;
  private final String pattern;
  private final Handler handler;

  public Route(String method, String pattern, Handler handler) {
    this.method = method;
    this.pattern = pattern;
    this.handler = handler;
  }

  public String method() {
    return method;
  }

  public String pattern() {
    return pattern;
  }

  @Override public String toString() {
    return method + " " + pattern;
  }
}
