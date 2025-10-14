/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;

import java.util.*;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocCommentsTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.ResponseExt;
import io.swagger.v3.oas.models.security.SecurityRequirement;

public class MethodDoc extends JavaDocNode {
  private Map<String, String> parameters;
  private List<SecurityRequirement> securityRequeriments;
  private String operationId;
  private Map<StatusCode, ResponseExt> throwList;
  private List<String> parameterTypes = null;
  private String returnDoc;

  public MethodDoc(JavaDocParser ctx, DetailAST node, DetailAST javadoc) {
    super(ctx, node, javadoc);
    throwList = JavaDocTag.throwList(this.javadoc);
    operationId = JavaDocTag.operationId(this.javadoc);
    securityRequeriments = JavaDocTag.securityRequirement(this.javadoc);
    parameters = JavaDocTag.getParametersDoc(this.javadoc);
    returnDoc = JavaDocTag.getReturnDoc(this.javadoc);
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

  public List<SecurityRequirement> getSecurityRequirements() {
    return securityRequeriments;
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
        .filter(javadocToken(JavadocCommentsTokenTypes.JAVADOC_BLOCK_TAG))
        .flatMap(
            it ->
                tree(it)
                    .filter(javadocToken(JavadocCommentsTokenTypes.PARAMETER_NAME))
                    .map(DetailNode::getText))
        .toList();
  }

  public String getParameterDoc(String name) {
    var doc = parameters.get(name);
    return doc == null ? null : doc.replace(exampleCode(doc), "").trim();
  }

  public Object getParameterExample(String name) {
    return toExamples(exampleCode(parameters.get(name)));
  }

  public String getReturnDoc() {
    if (returnDoc != null) {
      var result = returnDoc.replace(exampleCode(returnDoc), "").trim();
      return result.isEmpty() ? null : result;
    }
    return null;
  }

  public Object getReturnExample() {
    return toExamples(returnDoc);
  }

  public Map<StatusCode, ResponseExt> getThrows() {
    return throwList;
  }
}
