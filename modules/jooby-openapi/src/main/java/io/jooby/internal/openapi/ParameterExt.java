/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ParameterExt extends io.swagger.v3.oas.models.parameters.Parameter {
  /* keep track of expanded query bean parameters. */
  @JsonIgnore private String containerType;

  @JsonIgnore private String javaType;

  @JsonIgnore private Object defaultValue;

  @JsonIgnore private boolean single = true;

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getJavaType() {
    return javaType;
  }

  public void setContainerType(String containerType) {
    this.containerType = containerType;
  }

  public String getContainerType() {
    return containerType;
  }

  public Object getDefaultValue() {
    if (defaultValue != null) {
      if (javaType.equals(boolean.class.getName())) {
        return Objects.equals(defaultValue, 1);
      }
    }
    return defaultValue;
  }

  public boolean isSingle() {
    return single;
  }

  public void setSingle(boolean single) {
    this.single = single;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public void setRequired(Boolean required) {
    super.setRequired(required);
  }

  @Override
  public String toString() {
    return javaType + " " + getName();
  }
}
