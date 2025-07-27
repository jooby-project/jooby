/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodDoc extends JavaDocNode {
  private final DetailAST node;

  public MethodDoc(DetailAST node, DetailAST javadoc) {
    super(javadoc);
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
}
