package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class RamlParameter {
  private String name;

  private RamlType type;

  private Boolean required;

  private String description;

  private Object defaultValue;
  private List<String> enums;

  public RamlParameter(String name) {
    this.name = name;
  }

  @JsonIgnore
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @JsonIgnore
  public RamlType getType() {
    return type;
  }

  public void setType(final RamlType type) {
    this.type = type;
  }

  public Boolean isRequired() {
    return required;
  }

  public void setRequired(final Boolean required) {
    this.required = required;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public RamlParameter description(final String description) {
    this.description = description;
    return this;
  }

  public Object getDefault() {
    return defaultValue;
  }

  public void setDefault(final Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public RamlParameter type(final RamlType type) {
    this.type = type;
    return this;
  }

  @JsonAnyGetter
  Map<String, Object> attributes() {
    return ImmutableMap.of("type", type.getRef().getType());
  }

  public List<String> getEnum() {
    return enums;
  }

  public void setEnum(final List<String> enums) {
    this.enums = enums;
  }
}
