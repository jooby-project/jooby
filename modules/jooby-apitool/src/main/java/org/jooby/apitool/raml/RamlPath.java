package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class RamlPath {
  private String displayName;

  private String description;

  private Map<String, RamlMethod> methods;

  private Map<String, RamlPath> resources;

  private Map<String, RamlParameter> uriParameters;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @JsonAnyGetter
  public Map<String, Object> getChildren() {
    Map<String, Object> children = new LinkedHashMap<>();
    Optional.ofNullable(methods).ifPresent(children::putAll);
    Optional.ofNullable(resources).ifPresent(children::putAll);
    return children;
  }

  public RamlMethod method(String method) {
    if (methods == null) {
      methods = new LinkedHashMap<>();
    }
    String methodlower = method.toLowerCase();
    RamlMethod value = methods.get(methodlower);
    if (value == null) {
      value = new RamlMethod(methodlower);
      methods.put(methodlower, value);
    }
    return value;
  }

  public void setMethods(final Map<String, RamlMethod> methods) {
    this.methods = methods;
  }

  public void setResources(final Map<String, RamlPath> resources) {
    this.resources = resources;
  }

  public Map<String, RamlParameter> getUriParameters() {
    return uriParameters;
  }

  public void setUriParameters(final Map<String, RamlParameter> uriParameters) {
    this.uriParameters = uriParameters;
  }

  public RamlParameter uriParameter(String name) {
    if (uriParameters == null) {
      uriParameters = new LinkedHashMap<>();
    }
    RamlParameter param = uriParameters.get(name);
    if (param == null) {
      param = new RamlParameter(name);
      uriParameters.put(name, param);
    }
    return param;
  }

  public RamlPath path(String pattern) {
    if (resources == null) {
      resources = new LinkedHashMap<>();
    }
    RamlPath path = resources.get(pattern);
    if (path == null) {
      path = new RamlPath();
      resources.put(pattern, path);
    }
    return path;
  }

}
