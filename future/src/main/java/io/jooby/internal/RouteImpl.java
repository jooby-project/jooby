package io.jooby.internal;

import io.jooby.Route;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RouteImpl implements Route {

  private final String method;
  private final String pattern;
  private final Handler handler;
  private final Route.RootHandler pipeline;
  private final Map<String, String> params;
  List<String> paramKeys;

  public RouteImpl(String method, String pattern, Handler handler, Route.RootHandler pipeline) {
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.params = Collections.EMPTY_MAP;
    this.handler = handler;
    this.pipeline = pipeline;
  }

  private RouteImpl(String method, String pattern, Map<String, String> params, Handler handler, Route.RootHandler pipeline) {
    this.method = method;
    this.pattern = pattern;
    this.params = params;
    this.handler = handler;
    this.pipeline = pipeline;
  }

  @Override public Map<String, String> params() {
    return params;
  }

  @Override public String pattern() {
    return pattern;
  }

  @Override public String method() {
    return method;
  }

  @Override public Handler handler() {
    return handler;
  }

  @Override public Route.RootHandler pipeline() {
    return pipeline;
  }

  Route newRuntimeRoute(String method, Map<String, String> params) {
    return new RouteImpl(method, pattern, params, handler, pipeline);
  }

  @Override public String toString() {
    return method + " " + pattern;
  }
}
