/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.util.*;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.jooby.StatusCode;

public class MethodDoc extends JavaDocNode {

  private String operationId;
  private Map<StatusCode, ThrowsDoc> throwList;

  public MethodDoc(JavaDocParser ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
    throwList = JavaDocTag.throwList(this.javadoc);
    operationId = JavaDocTag.operationId(this.javadoc);
  }

  MethodDoc(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    super(ctx, node, javadoc);
  }

  public String getName() {
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
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

  public List<String> getJavadocParameterNames() {
    return tree(javadoc)
        // must be a tag
        .filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG))
        .flatMap(
            it ->
                children(it)
                    .filter(javadocToken(JavadocTokenTypes.PARAMETER_NAME))
                    .map(DetailNode::getText))
        .toList();
  }

  public String getParameterDoc(String name) {
    return tree(javadoc)
        // must be a tag
        .filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG))
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

  public Map<StatusCode, ThrowsDoc> getThrows() {
    return throwList;
  }
}
