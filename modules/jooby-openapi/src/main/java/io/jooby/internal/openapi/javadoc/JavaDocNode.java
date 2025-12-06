/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.javadocToken;

import java.util.*;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.DetailNodeTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocCommentsTokenTypes;
import io.swagger.v3.oas.models.tags.Tag;

public class JavaDocNode {
  private static final Predicate<DetailNode> JAVADOC_TAG =
      javadocToken(JavadocCommentsTokenTypes.JAVADOC_BLOCK_TAG);

  protected final JavaDocParser context;
  protected final DetailAST node;
  protected final DetailNode javadoc;
  private final Map<String, Object> extensions;
  private final List<Tag> tags;

  public JavaDocNode(JavaDocParser ctx, DetailAST node, DetailAST comment) {
    this(ctx, node, toJavaDocNode(comment));
  }

  protected JavaDocNode(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    this.context = ctx;
    this.node = node;
    this.javadoc = javadoc;
    this.tags = JavaDocTag.tags(javadoc);
    this.extensions = JavaDocTag.extensions(javadoc);
  }

  static DetailNode toJavaDocNode(DetailAST node) {
    return node == EMPTY_AST
        ? EMPTY_NODE
        : new JavadocDetailNodeParser().parseJavadocComment(node).getTree();
  }

  public DetailAST getNode() {
    return node;
  }

  public Map<String, Object> getExtensions() {
    return extensions;
  }

  public String getSummary() {
    var summary = ContentSplitter.split(getText()).summary();
    return summary.isEmpty() ? null : summary;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public String getDescription() {
    var description = ContentSplitter.split(getText()).description();
    return description.isEmpty() ? null : description;
  }

  public String getText() {
    return getText(JavaDocStream.forward(javadoc, JAVADOC_TAG).toList(), false);
  }

  protected String exampleCode(String text) {
    if (text == null) {
      return "";
    }
    var start = text.indexOf("`");
    if (start == -1) {
      return "";
    }
    var end = text.indexOf("`", start + 1);
    if (end == -1) {
      return "";
    }
    return text.substring(start, end + 1);
  }

  protected Object toExamples(String text) {
    var codeExample = exampleCode(text);
    if (codeExample.isEmpty()) {
      return null;
    }
    var clean = codeExample.substring(1, codeExample.length() - 1);
    var result = JavaDocObjectParser.parseJson(clean);
    if (result.equals(codeExample)) {
      // Like a primitive/basic example
      return List.of(result);
    }
    return result;
  }

  protected static String getText(List<DetailNode> nodes, boolean stripLeading) {
    var builder = new StringBuilder();
    var visited = new HashSet<DetailNode>();
    for (var node : nodes) {
      if (visited.add(node)) {
        if (node.getType() == JavadocCommentsTokenTypes.TEXT) {
          var text = node.getText();
          if (stripLeading && Character.isWhitespace(text.charAt(0))) {
            builder.append(' ').append(text.stripLeading());
          } else {
            builder.append(text);
          }
        } else if (node.getType() == JavadocCommentsTokenTypes.NEWLINE) {
          var next = node.getNextSibling();
          if (next != null && next.getType() != JavadocCommentsTokenTypes.LEADING_ASTERISK) {
            builder.append(next.getText());
            visited.add(next);
          }
        } else if (node.getType() == JavadocCommentsTokenTypes.TAG_NAME) {
          // <p>?
          if (node.getText().equals("p")) {
            // keep so we can split summary from description
            builder.append("<p>");
          }
        }
      }
    }
    return builder.isEmpty() ? null : builder.toString().trim().replaceAll("\\s+", " ");
  }

  protected static String toString(DetailNode node) {
    return DetailNodeTreeStringPrinter.printTree(node, "", "");
  }

  public static final DetailNode EMPTY_NODE =
      new DetailNode() {
        @Override
        public int getType() {
          return JavadocCommentsTokenTypes.JAVADOC_CONTENT;
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

        //        @Override
        //        public DetailNode[] getChildren() {
        //          return new DetailNode[0];
        //        }

        @Override
        public DetailNode getParent() {
          return null;
        }

        @Override
        public DetailNode getNextSibling() {
          return null;
        }

        @Override
        public DetailNode getFirstChild() {
          return null;
        }

        @Override
        public DetailNode getPreviousSibling() {
          return null;
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
}
