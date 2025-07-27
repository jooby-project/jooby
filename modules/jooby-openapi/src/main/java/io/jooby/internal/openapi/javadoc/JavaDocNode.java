/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import java.util.List;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.DetailNodeTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

public class JavaDocNode {
  protected final DetailNode javadoc;
  protected static final Set<Integer> STOP_TOKENS = Set.of(JavadocTokenTypes.JAVADOC_TAG);

  public JavaDocNode(DetailAST node) {
    this.javadoc = new JavadocDetailNodeParser().parseJavadocAsDetailNode(node).getTree();
  }

  public String getText() {
    return getText(JavaDocSupport.forward(javadoc, STOP_TOKENS).toList(), false);
  }

  protected String getText(List<DetailNode> nodes, boolean stripLeading) {
    var builder = new StringBuilder();
    for (var node : nodes) {
      if (node.getType() == JavadocTokenTypes.TEXT) {
        var text = node.getText();
        if (stripLeading && Character.isWhitespace(text.charAt(0))) {
          builder.append(' ').append(text.stripLeading());
        } else {
          builder.append(text);
        }
      } else if (node.getType() == JavadocTokenTypes.NEWLINE) {
        var next = JavadocUtil.getNextSibling(node);
        if (next != null && next.getType() != JavadocTokenTypes.LEADING_ASTERISK) {
          builder.append(next.getText());
        }
      }
    }
    return builder.toString().trim();
  }

  @Override
  public String toString() {
    return DetailNodeTreeStringPrinter.printTree(javadoc, "", "");
  }
}
