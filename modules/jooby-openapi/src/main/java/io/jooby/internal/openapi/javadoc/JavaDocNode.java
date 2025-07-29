/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.forward;

import java.util.List;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.DetailNodeTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

public class JavaDocNode {
  public static final DetailNode EMPTY_NODE =
      new DetailNode() {
        @Override
        public int getType() {
          return 0;
        }

        @Override
        public String getText() {
          return "";
        }

        @Override
        public int getLineNumber() {
          return 0;
        }

        @Override
        public int getColumnNumber() {
          return 0;
        }

        @Override
        public DetailNode[] getChildren() {
          return new DetailNode[0];
        }

        @Override
        public DetailNode getParent() {
          return null;
        }

        @Override
        public int getIndex() {
          return 0;
        }
      };

  public static final DetailAST EMPTY_AST =
      new DetailAST() {
        @Override
        public int getChildCount() {
          return 0;
        }

        @Override
        public int getChildCount(int type) {
          return 0;
        }

        @Override
        public DetailAST getParent() {
          return null;
        }

        @Override
        public String getText() {
          return "";
        }

        @Override
        public int getType() {
          return 0;
        }

        @Override
        public int getLineNo() {
          return 0;
        }

        @Override
        public int getColumnNo() {
          return 0;
        }

        @Override
        public DetailAST getLastChild() {
          return null;
        }

        @Override
        public boolean branchContains(int type) {
          return false;
        }

        @Override
        public DetailAST getPreviousSibling() {
          return null;
        }

        @Override
        public DetailAST findFirstToken(int type) {
          return null;
        }

        @Override
        public DetailAST getNextSibling() {
          return null;
        }

        @Override
        public DetailAST getFirstChild() {
          return null;
        }

        @Override
        public int getNumberOfChildren() {
          return 0;
        }

        @Override
        public boolean hasChildren() {
          return false;
        }
      };

  protected final JavaDocContext context;
  protected final DetailNode javadoc;
  private static final Predicate<DetailNode> JAVADOC_TAG =
      JavaDocSupport.javadocToken(JavadocTokenTypes.JAVADOC_TAG);

  public JavaDocNode(JavaDocContext ctx, DetailAST node) {
    this.context = ctx;
    this.javadoc = toJavaDocNode(node);
  }

  static DetailNode toJavaDocNode(DetailAST node) {
    return node == EMPTY_AST
        ? EMPTY_NODE
        : new JavadocDetailNodeParser().parseJavadocAsDetailNode(node).getTree();
  }

  public String getSummary() {
    var builder = new StringBuilder();
    for (var node : forward(javadoc, JAVADOC_TAG).toList()) {
      if (node.getType() == JavadocTokenTypes.TEXT) {
        var text = node.getText();
        var trimmed = text.trim();
        if (trimmed.isEmpty()) {
          if (!builder.isEmpty()) {
            builder.append(text);
          }
        } else {
          builder.append(text);
        }
      } else if (node.getType() == JavadocTokenTypes.NEWLINE && !builder.isEmpty()) {
        break;
      }
      var index = builder.indexOf(".");
      if (index > 0) {
        builder.setLength(index + 1);
        break;
      }
    }
    var string = builder.toString().trim();
    return string.isEmpty() ? null : string;
  }

  public String getDescription() {
    var text = getText();
    var summary = getSummary();
    if (summary == null) {
      return text;
    }
    return summary.equals(text) ? null : text.replaceAll(summary, "").trim();
  }

  protected String getText() {
    return getText(JavaDocSupport.forward(javadoc, JAVADOC_TAG).toList(), false);
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
    return builder.isEmpty() ? null : builder.toString().trim();
  }

  @Override
  public String toString() {
    return toString(javadoc);
  }

  protected String toString(DetailNode node) {
    return DetailNodeTreeStringPrinter.printTree(node, "", "");
  }
}
