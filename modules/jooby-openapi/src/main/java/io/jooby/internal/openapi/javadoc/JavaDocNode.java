/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.javadocToken;

import java.util.*;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.DetailNodeTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

public class JavaDocNode {
  private static final Predicate<DetailNode> JAVADOC_TAG =
      javadocToken(JavadocTokenTypes.JAVADOC_TAG);

  protected final JavaDocParser context;
  protected final DetailAST node;
  protected final DetailNode javadoc;
  private final Map<String, Object> extensions;
  private final Map<String, String> tags;

  public JavaDocNode(JavaDocParser ctx, DetailAST node, DetailAST comment) {
    this(ctx, node, toJavaDocNode(comment));
  }

  protected JavaDocNode(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    this.context = ctx;
    this.node = node;
    this.javadoc = javadoc;
    if (this.javadoc != EMPTY_NODE) {
      this.extensions = parseExtensions(this.javadoc);
      this.tags = parseTags(this.javadoc);
    } else {
      this.extensions = Map.of();
      this.tags = Map.of();
    }
  }

  private Map<String, String> parseTags(DetailNode node) {
    var result = new LinkedHashMap<String, String>();
    for (var docTag : tree(node).filter(JAVADOC_TAG).toList()) {
      var tag =
          tree(docTag)
              .filter(
                  javadocToken(JavadocTokenTypes.CUSTOM_NAME)
                      .and(it -> it.getText().equals("@tag")))
              .findFirst()
              .orElse(null);
      if (tag != null) {
        var tagText =
            tree(docTag)
                .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                .findFirst()
                .map(it -> getText(List.of(it.getChildren()), false))
                .orElse(null);
        if (tagText != null) {
          var dot = tagText.indexOf(".");
          var tagName = tagText;
          String tagDescription = null;
          if (dot > 0) {
            tagName = tagText.substring(0, dot);
            if (dot + 1 < tagText.length()) {
              tagDescription = tagText.substring(dot + 1).trim();
              if (tagDescription.isBlank()) {
                tagDescription = null;
              }
            }
          }
          if (!tagName.trim().isEmpty()) {
            result.put(tagName, tagDescription);
          }
        }
      }
    }
    return result;
  }

  private Map<String, Object> parseExtensions(DetailNode node) {
    var values = new ArrayList<String>();
    for (var tag : tree(node).filter(JAVADOC_TAG).toList()) {
      var extension =
          tree(tag)
              .filter(
                  javadocToken(JavadocTokenTypes.CUSTOM_NAME)
                      .and(it -> it.getText().startsWith("@x-")))
              .findFirst()
              .map(DetailNode::getText)
              .orElse(null);
      if (extension != null) {
        extension = extension.substring(1).trim();
        var extensionValue =
            tree(tag)
                .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                .findFirst()
                .map(it -> getText(List.of(it.getChildren()), false))
                .orElse(null);
        values.add(extension);
        values.add(extensionValue);
      }
    }
    return ExtensionJavaDocParser.parse(values);
  }

  static DetailNode toJavaDocNode(DetailAST node) {
    return node == EMPTY_AST
        ? EMPTY_NODE
        : new JavadocDetailNodeParser().parseJavadocAsDetailNode(node).getTree();
  }

  public Map<String, Object> getExtensions() {
    return extensions;
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

  public Map<String, String> getTags() {
    return tags;
  }

  public String getDescription() {
    var text = getText();
    var summary = getSummary();
    if (summary == null) {
      return text;
    }
    return summary.equals(text) ? null : text.replaceAll(summary, "").trim();
  }

  public String getText() {
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

  protected String toString(DetailNode node) {
    return DetailNodeTreeStringPrinter.printTree(node, "", "");
  }

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
          return JavadocTokenTypes.TEXT;
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
