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

  private Map<StatusCode, ThrowsDoc> throwList;

  public MethodDoc(JavaDocParser ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
    throwList = throwList(this.javadoc);
  }

  private Map<StatusCode, ThrowsDoc> throwList(DetailNode javadoc) {
    var result = new LinkedHashMap<StatusCode, ThrowsDoc>();
    for (var tag : tree(javadoc).filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG)).toList()) {
      var isThrows = tree(tag).anyMatch(javadocToken(JavadocTokenTypes.THROWS_LITERAL));
      if (isThrows) {
        var text =
            tree(tag)
                .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                .findFirst()
                .map(it -> getText(List.of(it.getChildren()), true))
                .orElse(null);
        var statusCode =
            tree(tag)
                .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                .findFirst()
                .flatMap(
                    it ->
                        tree(it)
                            .filter(javadocToken(JavadocTokenTypes.HTML_TAG_NAME))
                            .filter(tagName -> tagName.getText().equals("code"))
                            .flatMap(
                                tagName ->
                                    backward(tagName)
                                        .filter(javadocToken(JavadocTokenTypes.HTML_TAG))
                                        .findFirst()
                                        .stream())
                            .flatMap(
                                htmlTag ->
                                    children(htmlTag)
                                        .filter(javadocToken(JavadocTokenTypes.TEXT))
                                        .findFirst()
                                        .stream())
                            .map(DetailNode::getText)
                            .map(
                                value -> {
                                  try {
                                    return Integer.parseInt(value);
                                  } catch (NumberFormatException e) {
                                    return null;
                                  }
                                })
                            .filter(Objects::nonNull)
                            .filter(code -> code >= 400 && code <= 600)
                            .map(StatusCode::valueOf)
                            .findFirst())
                .orElse(null);
        //        var className = tree(tag).filter(javadocToken(JavadocTokenTypes.CLASS_NAME))
        //            .findFirst()
        //            .map(DetailNode::getText)
        //            .orElse(null);
        if (statusCode != null) {
          var throwsDoc = new ThrowsDoc(statusCode, text);
          result.putIfAbsent(statusCode, throwsDoc);
        }
      }
    }
    return result;
  }

  MethodDoc(JavaDocParser ctx, DetailAST node, DetailNode javadoc) {
    super(ctx, node, javadoc);
  }

  public String getName() {
    return node.findFirstToken(TokenTypes.IDENT).getText();
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
      return context.parse(in).map(bean -> bean.getPropertyDoc(name)).orElse(null);
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

  public Map<StatusCode, ThrowsDoc> getThrows() {
    return throwList;
  }
}
