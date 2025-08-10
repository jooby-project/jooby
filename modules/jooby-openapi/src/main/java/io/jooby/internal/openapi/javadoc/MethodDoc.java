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
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
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
        .filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG))
        .flatMap(
            it ->
                children(it)
                    .filter(javadocToken(JavadocTokenTypes.PARAMETER_NAME))
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

  private String exampleCode(String text) {
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

  private Object toExamples(String text) {
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

  public String getReturnDoc() {
    if (returnDoc != null) {
      return returnDoc.replace(exampleCode(returnDoc), "").trim();
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
