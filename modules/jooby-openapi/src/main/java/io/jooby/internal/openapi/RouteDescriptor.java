package io.jooby.internal.openapi;

import io.jooby.MediaType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RouteDescriptor {

  private final String method;
  private final String pattern;
  private final List<RouteArgument> arguments;
  private final List<RouteReturnType> returnTypes;
  private final LinkedList<String> produces = new LinkedList<>();
  private final LinkedList<String> consumes = new LinkedList<>();

  public RouteDescriptor(String method, String pattern, List<RouteArgument> arguments, List<RouteReturnType> returnTypes) {
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.arguments = arguments;
    this.returnTypes = returnTypes;
  }

  public List<RouteArgument> getArguments() {
    return arguments;
  }

  public RouteReturnType getReturnType() {
    return getReturnTypes().get(0);
  }

  public List<RouteReturnType> getReturnTypes() {
    return returnTypes;
  }

  public String getMethod() {
    return method;
  }

  public String getPattern() {
    return pattern;
  }

  public List<String> getProduces() {
    return produces;
  }

  public List<String> getConsumes() {
    return consumes;
  }

  public void addProduces(String value) {
    produces.addFirst(toMediaType(value));
  }

  public void addConsumes(String value) {
    consumes.addFirst(toMediaType(value));
  }

  private String toMediaType(String value) {
    return MediaType.valueOf(value).toString();
  }

  public String toString() {
    return getMethod() + " " + getPattern();
  }
}
