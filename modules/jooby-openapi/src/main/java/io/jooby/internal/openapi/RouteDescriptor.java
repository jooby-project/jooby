package io.jooby.internal.openapi;

import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.stream.Collectors;

public class RouteDescriptor {

  private final String method;
  private final String pattern;
  private final List<RouteArgument> arguments;
  private final List<RouteReturnType> returnTypes;

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

  public String toString() {
    return getMethod() + " " + getPattern();
  }
}
