/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodDoc extends JavaDocNode {
  private final DetailAST node;

  public MethodDoc(JavaDocContext ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, javadoc);
    this.node = node;
  }

  public String getName() {
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  public List<String> getParameterTypes() {
    var result = new ArrayList<String>();
    for (var parameterDef : tree(node).filter(tokens(TokenTypes.PARAMETER_DEF)).toList()) {
      var typeNode =
          tree(parameterDef)
              .filter(tokens(TokenTypes.TYPE))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("Parameter type not found: " + parameterDef));
      if (typeNode.getFirstChild().getType() == TokenTypes.DOT) {
        result.add(typeNode.getFirstChild().getLastChild().getText());
      } else {
        result.add(typeNode.getFirstChild().getText());
      }
    }
    return result;
  }

  public List<String> getParameterNames() {
    var result = new ArrayList<String>();
    var index = 0;
    for (var parameterDef : tree(node).filter(tokens(TokenTypes.PARAMETER_DEF)).toList()) {
      var name =
          children(parameterDef)
              .filter(tokens(TokenTypes.IDENT))
              .findFirst()
              .map(DetailAST::getText)
              .orElse("param" + index);
      result.add(name);
      index++;
    }
    return result;
  }

  public String getParameterDoc(String name) {
    return getParameterDoc(name, null);
  }

  public String getParameterDoc(String name, String in) {
    if (in != null) {
      var tree = context.resolve(toJavaPath(in));
      if (tree == JavaDocContext.NULL) {
        return null;
      }
      return getPropertyDoc(tree, name);
    }
    return tree(javadoc)
        // must be a tag
        .filter(it -> it.getType() == JavadocTokenTypes.JAVADOC_TAG)
        .filter(
            it -> {
              var children = children(it).toList();
              return children.stream()
                      .anyMatch(
                          t ->
                              t.getType() == JavadocTokenTypes.PARAM_LITERAL
                                  && t.getText().equals("@param"))
                  && children.stream().anyMatch(t -> t.getText().equals(name));
            })
        .findFirst()
        .map(
            it ->
                getText(
                    Stream.of(it.getChildren())
                        .filter(e -> e.getType() == JavadocTokenTypes.DESCRIPTION)
                        .flatMap(JavaDocSupport::tree)
                        .toList(),
                    true))
        .filter(it -> !it.isEmpty())
        .orElse(null);
  }

  public String getReturnDoc() {
    return tree(javadoc)
        .filter(javadocToken(JavadocTokenTypes.RETURN_LITERAL))
        .findFirst()
        .flatMap(
            it ->
                tree(it.getParent())
                    .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                    .findFirst())
        .map(it -> getText(tree(it).toList(), true))
        .orElse(null);
  }

  private Path toJavaPath(String in) {
    var segments = in.split("\\.");
    segments[segments.length - 1] = segments[segments.length - 1] + ".java";
    return Paths.get(String.join(File.separator, segments));
  }

  private String getPropertyDoc(DetailAST bean, String name) {
    String comment;
    var isRecord = tree(bean).anyMatch(tokens(TokenTypes.RECORD_DEF));
    if (isRecord) {
      comment = commentFromRecord(bean, name);
    } else {
      comment = commentFromGetter(bean, name);
      if (comment == null) {
        comment = commentFromField(bean, name);
      }
    }
    return comment;
  }

  private String commentFromRecord(DetailAST bean, String name) {
    var commentNode =
        tree(bean)
            .filter(tokens(TokenTypes.RECORD_DEF))
            .findFirst()
            .flatMap(it -> Optional.ofNullable(commentFromMember(it)))
            .map(JavaDocNode::toJavaDocNode)
            .orElse(null);
    if (commentNode == null) {
      return null;
    }

    for (var tag : tree(commentNode).filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG)).toList()) {
      var isParam = tree(tag).anyMatch(javadocToken(JavadocTokenTypes.PARAM_LITERAL));
      var matchesName =
          tree(tag)
              .filter(javadocToken(JavadocTokenTypes.PARAMETER_NAME))
              .findFirst()
              .filter(it -> it.getText().equals(name))
              .isPresent();
      if (isParam && matchesName) {
        return getText(
            tree(tag)
                .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                .flatMap(it -> Stream.of(it.getChildren()))
                .toList(),
            true);
      }
    }

    return null;
  }

  private String commentFromGetter(DetailAST bean, String name) {
    var methods = JavaDocSupport.forward(bean).filter(tokens(TokenTypes.METHOD_DEF)).toList();
    var getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    for (var method : methods) {
      var noArgs = tree(method).noneMatch(tokens(TokenTypes.PARAMETER_DEF));
      var isPublic = tree(method).anyMatch(tokens(TokenTypes.LITERAL_PUBLIC));
      var methodName =
          children(method)
              .filter(tokens(TokenTypes.IDENT))
              .map(DetailAST::getText)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Method name not found"));
      if (noArgs && isPublic && (methodName.equals(getterName) || methodName.equals(name))) {
        var comment = commentFromMember(method);
        if (comment != null) {
          return textFromComment(comment);
        }
      }
    }
    return null;
  }

  private String textFromComment(DetailAST comment) {
    return comment == null ? null : new JavaDocNode(context, comment).getText();
  }

  private String commentFromField(DetailAST bean, String name) {
    for (var field :
        JavaDocSupport.forward(bean).filter(tokens(TokenTypes.VARIABLE_DEF)).toList()) {
      var isInstance = tree(field).noneMatch(tokens(TokenTypes.LITERAL_STATIC));
      var fieldName =
          children(field)
              .filter(tokens(TokenTypes.IDENT))
              .map(DetailAST::getText)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Field name not found"));
      if (isInstance && fieldName.equals(name)) {
        var comment = commentFromMember(field);
        if (comment != null) {
          return textFromComment(comment);
        }
      }
    }
    return null;
  }

  private static DetailAST commentFromMember(DetailAST member) {
    var modifiers = tree(member).filter(tokens(TokenTypes.MODIFIERS)).findFirst().orElse(null);
    if (modifiers != null) {
      return tree(modifiers)
          .filter(tokens(TokenTypes.BLOCK_COMMENT_BEGIN))
          .findFirst()
          .orElse(null);
    }
    return null;
  }
}
