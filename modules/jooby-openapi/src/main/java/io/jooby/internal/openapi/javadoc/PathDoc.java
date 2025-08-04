/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class PathDoc extends JavaDocNode {
  public PathDoc(JavaDocParser ctx, DetailAST node, DetailAST comment) {
    super(ctx, node, comment);
  }
}
