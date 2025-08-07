/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class ScriptDoc extends MethodDoc {
  private final String method;
  private final String pattern;
  private PathDoc path;

  public ScriptDoc(
      JavaDocParser ctx, String method, String pattern, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
    this.method = method;
    this.pattern = pattern;
  }

  public PathDoc getPath() {
    return path;
  }

  public void setPath(PathDoc path) {
    this.path = path;
  }

  public String getMethod() {
    return method;
  }

  public String getPattern() {
    return pattern;
  }

  @Override
  public String toString() {
    return method + " " + pattern;
  }
}
