/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.jooby.javadoc.JavaDocNode;
import io.jooby.javadoc.MethodDoc;
import io.jooby.javadoc.ScriptDoc;
import io.swagger.v3.oas.models.parameters.Parameter;

public class JavaDocSetter {

  public static void setPath(OperationExt operation, JavaDocNode doc) {
    operation.setPathDescription(doc.getDescription());
    operation.setPathSummary(doc.getSummary());
    JavaDocMapper.toTags(doc.getTags()).forEach(operation::addTag);
    if (!doc.getExtensions().isEmpty()) {
      operation.setPathExtensions(doc.getExtensions());
    }
  }

  public static void set(OperationExt operation, ScriptDoc doc) {
    var parameters = Optional.ofNullable(operation.getParameters()).orElse(List.of());
    var parameterNames = parameters.stream().map(Parameter::getName).collect(Collectors.toList());
    if (operation.getRequestBody() != null) {
      var javaDocNames = new LinkedHashSet<>(doc.getJavadocParameterNames());
      parameterNames.forEach(javaDocNames::remove);
      if (javaDocNames.size() == 1) {
        // just add body name on lambda/script routes.
        parameterNames.addAll(javaDocNames);
      }
    }
    set(operation, doc, parameterNames);
  }

  public static void set(OperationExt operation, MethodDoc doc, List<String> parameterNames) {
    operation.setOperationId(
        Optional.ofNullable(operation.getOperationId()).orElse(doc.getOperationId()));
    operation.setSummary(doc.getSummary());
    operation.setDescription(doc.getDescription());
    if (!doc.getExtensions().isEmpty()) {
      operation.setExtensions(doc.getExtensions());
    }
    JavaDocMapper.toSecurityRequirements(doc.getSecurityRequirements())
        .forEach(operation::addSecurityItem);
    JavaDocMapper.toTags(doc.getTags()).forEach(operation::addTag);
    // Parameters
    for (var parameterName : parameterNames) {
      var paramExt =
          operation.getParameters().stream()
              .filter(p -> p.getName().equals(parameterName))
              .findFirst()
              .map(ParameterExt.class::cast)
              .orElse(null);
      var paramDoc = doc.getParameterDoc(parameterName);
      if (paramDoc != null) {
        if (paramExt == null) {
          var body = operation.getRequestBody();
          if (body != null) {
            body.setExamples(doc.getParameterExample(parameterName));
            body.setDescription(paramDoc);
          }
        } else {
          paramExt.setExample(doc.getParameterExample(parameterName));
          paramExt.setDescription(paramDoc);
        }
      }
    }
    // return types
    var defaultResponse = operation.getDefaultResponse();
    if (defaultResponse != null) {
      defaultResponse.setExamples(doc.getReturnExample());
      defaultResponse.setDescription(doc.getReturnDoc());
    }
    for (var throwsDoc : doc.getThrows().entrySet()) {
      var response = operation.getResponse(Integer.toString(throwsDoc.getKey()));
      if (response == null) {
        response = operation.addResponse(Integer.toString(throwsDoc.getKey()));
      }
      response.setDescription(throwsDoc.getValue());
    }
  }
}
