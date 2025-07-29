/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class ClassDoc extends JavaDocNode {

  private final DetailAST node;
  private final List<MethodDoc> methods = new ArrayList<>();

  public ClassDoc(JavaDocContext ctx, DetailAST node, DetailAST javaDoc) {
    super(ctx, javaDoc);
    this.node = node;
  }

  public void addMethod(MethodDoc method) {
    this.methods.add(method);
  }

  public Optional<MethodDoc> getMethod(String name, List<String> parameterNames) {
    var filtered = methods.stream().filter(it -> it.getName().equals(name)).toList();
    if (filtered.isEmpty()) {
      return Optional.empty();
    }
    if (filtered.size() == 1) {
      return Optional.of(filtered.getFirst());
    }
    return filtered.stream()
        .filter(it -> it.getParameterNames().equals(parameterNames))
        .findFirst();
  }

  public String getSimpleName() {
    return node.findFirstToken(TokenTypes.IDENT).getText();
  }

  public String getName() {
    return forward(node.getParent())
            .filter(tokens(TokenTypes.PACKAGE_DEF))
            .map(
                it ->
                    tree(it)
                        .filter(tokens(TokenTypes.DOT, TokenTypes.IDENT))
                        .findFirst()
                        .orElse(null))
            .filter(Objects::nonNull)
            .flatMap(
                it ->
                    tree(it)
                        .filter(tokens(TokenTypes.DOT, TokenTypes.SEMI).negate())
                        .map(DetailAST::getText))
            .collect(Collectors.joining(".", "", "."))
        + getSimpleName();
  }

  public List<MethodDoc> getMethods() {
    return methods;
  }
}
