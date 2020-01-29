package io.jooby.internal.openapi;

import org.objectweb.asm.tree.MethodNode;

public class RouteDescriptor {

  private String pattern;

  private MethodNode node;

  public RouteDescriptor(String pattern, MethodNode node) {
    this.pattern = pattern;
    this.node = node;
  }

  public MethodNode getNode() {
    return node;
  }

  public String getPattern() {
    return pattern;
  }

  public String toString() {
    return pattern;
  }
}
