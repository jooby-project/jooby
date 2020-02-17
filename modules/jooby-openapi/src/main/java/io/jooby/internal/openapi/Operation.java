package io.jooby.internal.openapi;

import io.jooby.MediaType;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Operation {

  private final MethodNode node;
  private String id;
  private Set<String> tags = new LinkedHashSet<>();
  private boolean deprecated;
  private final String method;
  private final String pattern;
  private final List<Parameter> arguments;
  private List<OperationResponse> response;
  private final LinkedList<String> produces = new LinkedList<>();
  private final LinkedList<String> consumes = new LinkedList<>();
  private boolean hidden;
  private String summary;
  private String description;

  public Operation(MethodNode node, String method, String pattern, List<Parameter> arguments,
      List<OperationResponse> response) {
    this.node = node;
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.arguments = arguments;
    this.response = response;
  }

  public MethodNode getNode() {
    return node;
  }

  public List<Parameter> getArguments() {
    return arguments;
  }

  public OperationResponse getReturnType() {
    return getResponse().get(0);
  }

  public List<OperationResponse> getResponse() {
    return response;
  }

  public String getMethod() {
    return method;
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Set<String> getTags() {
    return tags;
  }

  public void addTag(String tag) {
    this.tags.add(tag);
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  public void addProduces(String value) {
    produces.addFirst(toMediaType(value));
  }

  public void addConsumes(String value) {
    consumes.addFirst(toMediaType(value));
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean value) {
    this.hidden = value;
  }

  private String toMediaType(String value) {
    return MediaType.valueOf(value).toString();
  }

  public String toString() {
    return getMethod() + " " + getPattern();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public void setResponse(List<OperationResponse> returnTypes) {
    this.response = returnTypes;
  }
}
