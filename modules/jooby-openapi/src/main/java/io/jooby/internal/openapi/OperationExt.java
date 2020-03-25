/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jooby.internal.openapi.StatusCodeParser.isSuccessCode;
import static java.util.Optional.ofNullable;

public class OperationExt extends io.swagger.v3.oas.models.Operation {

  @JsonIgnore
  private final MethodNode node;
  @JsonIgnore
  private String method;
  @JsonIgnore
  private final String pattern;
  @JsonIgnore
  private Boolean hidden;
  @JsonIgnore
  private LinkedList<String> produces = new LinkedList<>();
  @JsonIgnore
  private LinkedList<String> consumes = new LinkedList<>();
  @JsonIgnore
  private ResponseExt defaultResponse;
  @JsonIgnore
  private List<String> responseCodes = new ArrayList<>();
  @JsonIgnore
  private String pathSummary;
  @JsonIgnore
  private String pathDescription;
  @JsonIgnore
  private List<Tag> globalTags = new ArrayList<>();
  @JsonIgnore
  private ClassNode application;
  @JsonIgnore
  private ClassNode controller;

  public OperationExt(MethodNode node, String method, String pattern, List arguments,
      ResponseExt response) {
    this.node = node;
    this.method = method.toUpperCase();
    this.pattern = pattern;
    setParameters(arguments);
    this.defaultResponse = response;
    setResponses(apiResponses(Collections.singletonList(response)));
  }

  private static ApiResponses apiResponses(List<ResponseExt> responses) {
    ApiResponses result = new ApiResponses();
    for (ResponseExt rsp : responses) {
      result.addApiResponse(rsp.getCode(), rsp);
    }
    return result;
  }

  public MethodNode getNode() {
    return node;
  }

  @Override public RequestBodyExt getRequestBody() {
    return (RequestBodyExt) super.getRequestBody();
  }

  public ResponseExt getDefaultResponse() {
    return defaultResponse;
  }

  public List<String> getResponseCodes() {
    return responseCodes;
  }

  public ResponseExt getResponse(String code) {
    return (ResponseExt) getResponses().get(code);
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPattern() {
    return pattern;
  }

  public List<String> getProduces() {
    return produces;
  }

  public List<String> getConsumes() {
    return consumes;
  }

  public void addProduces(String value) {
    produces.addFirst(toMediaType(value));
  }

  public void addConsumes(String value) {
    consumes.addFirst(toMediaType(value));
  }

  private String toMediaType(String value) {
    return MediaType.valueOf(value).toString();
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public String toString() {
    return getMethod() + " " + getPattern();
  }

  public Parameter getParameter(int i) {
    if (i < getParameters().size()) {
      return getParameters().get(i);
    }
    return null;
  }

  public Optional<Parameter> getParameter(String name) {
    return getParameters().stream()
        .filter(p -> p.getName().equals(name))
        .findFirst();
  }

  public ResponseExt addResponse(String code) {
    responseCodes.add(code);
    return (ResponseExt) getResponses().computeIfAbsent(code, statusCode -> {
      ResponseExt rsp = new ResponseExt(statusCode);
      if (isSuccessCode(statusCode)) {
        rsp.setJavaTypes(defaultResponse.getJavaTypes());
      }
      return rsp;
    });
  }

  public String getPathDescription() {
    return pathDescription;
  }

  public void setPathDescription(String pathDescription) {
    this.pathDescription = pathDescription;
  }

  public String getPathSummary() {
    return pathSummary;
  }

  public void setPathSummary(String pathSummary) {
    this.pathSummary = pathSummary;
  }

  public void addTag(Tag tag) {
    this.globalTags.add(tag);
    addTagsItem(tag.getName());
  }

  public List<Tag> getGlobalTags() {
    return globalTags;
  }

  public void setGlobalTags(List<Tag> globalTags) {
    this.globalTags = globalTags;
  }

  public ClassNode getApplication() {
    return application;
  }

  public void setApplication(ClassNode application) {
    this.application = application;
  }

  public ClassNode getController() {
    return controller;
  }

  public void setController(ClassNode controller) {
    this.controller = controller;
  }

  @JsonIgnore
  public List<AnnotationNode> getAllAnnotations() {
    return Stream.of(
        ofNullable(controller).map(c -> c.visibleAnnotations)
            .orElse(application.visibleAnnotations),
        node.visibleAnnotations
    ).filter(Objects::nonNull)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  public OperationExt copy(String pattern) {
    OperationExt copy = new OperationExt(node, method, pattern, getParameters(), defaultResponse);
    copy.setTags(getTags());
    copy.setResponses(getResponses());
    copy.setParameters(getParameters());
    copy.setRequestBody(getRequestBody());
    copy.setHidden(getHidden());
    copy.setMethod(getMethod());
    copy.setDeprecated(getDeprecated());
    copy.setHidden(getHidden());
    copy.setDescription(getDescription());
    copy.setSummary(getSummary());
    copy.setOperationId(getOperationId());
    copy.setServers(getServers());
    copy.setCallbacks(getCallbacks());
    copy.setExternalDocs(getExternalDocs());
    copy.setSecurity(getSecurity());
    copy.setPathDescription(getPathDescription());
    copy.setPathSummary(getPathSummary());
    copy.setGlobalTags(getGlobalTags());
    copy.setApplication(getApplication());
    copy.setController(getController());
    return copy;
  }
}
