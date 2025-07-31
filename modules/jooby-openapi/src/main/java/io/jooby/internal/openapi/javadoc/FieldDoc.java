/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class FieldDoc extends JavaDocNode {
  public FieldDoc(JavaDocParser ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
  }

  FieldDoc(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    super(ctx, node, javadoc);
  }

  public String getName() {
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  @Override
  public String getText() {
    return super.getText();
  }
}
