package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;
import java.util.Optional;

public class Parameter extends io.swagger.v3.oas.models.parameters.Parameter {
  @JsonIgnore
  private String javaType;

  @JsonIgnore
  private Object defaultValue;

  @JsonIgnore
  private HttpType httpType;

  @JsonIgnore
  private boolean single = true;

  public HttpType getHttpType() {
    return httpType;
  }

  public void setHttpType(HttpType httpType) {
    this.httpType = httpType;
    Optional.ofNullable(this.httpType.in()).ifPresent(this::setIn);
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getJavaType() {
    return javaType;
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

  @Override public String toString() {
    return javaType + " " + getName();
  }
}
