/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;

import java.util.*;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.ResponseExt;

public class MethodDoc extends JavaDocNode {
  private String operationId;
  private Map<StatusCode, ResponseExt> throwList;
  private List<String> parameterTypes = null;

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

  public List<String> getParameterTypes() {
    if (parameterTypes == null) {
      parameterTypes = new ArrayList<>();
      var classDef =
          backward(node)
              .filter(JavaDocSupport.TYPES)
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Class not found: " + node));
      for (var parameter : tree(node).filter(tokens(TokenTypes.PARAMETER_DEF)).toList()) {
        var type =
            children(parameter)
                .filter(tokens(TokenTypes.TYPE))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Parameter type not found: "
                                + JavaDocSupport.getSimpleName(parameter)));
        parameterTypes.add(
            JavaDocSupport.toQualifiedName(classDef, JavaDocSupport.getQualifiedName(type)));
      }
    }
    return parameterTypes;
  }

  public MethodDoc markAsVirtual() {
    parameterTypes = List.of();
    return this;
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
                        .flatMap(JavaDocStream::tree)
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

  public Map<StatusCode, ResponseExt> getThrows() {
    return throwList;
  }
}
