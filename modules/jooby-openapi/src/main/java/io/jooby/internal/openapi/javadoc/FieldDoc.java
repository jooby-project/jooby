/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;

public class FieldDoc extends JavaDocNode {
  public FieldDoc(JavaDocParser ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
  }

  FieldDoc(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    super(ctx, node, javadoc);
  }

  @Override
  public String getText() {
    var text = super.getText();
    return text == null ? null : text.replace("<p>", "").replace("</p>", "").trim();
  }

  public String getName() {
    return JavaDocSupport.getSimpleName(node);
  }
}
