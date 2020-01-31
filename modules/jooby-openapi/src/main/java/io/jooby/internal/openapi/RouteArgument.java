package io.jooby.internal.openapi;

import java.util.Objects;

public class RouteArgument {
  private String name;

  private String javaType;

  private boolean required;

  private Object defaultValue;

  private HttpType httpType;

  private boolean single = true;

  public void setName(String name) {
    this.name = name;
  }

  public HttpType getHttpType() {
    return httpType;
  }

  public void setHttpType(HttpType httpType) {
    this.httpType = httpType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getName() {
    return name;
  }

  public String getJavaType() {
    return javaType;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isSingle() {
    return single;
  }

  public void setSingle(boolean single) {
    this.single = single;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public Object getDefaultValue() {
    if (defaultValue != null) {
      if (javaType.equals(boolean.class.getName())) {
        return Objects.equals(defaultValue, 1);
      }
    }
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override public String toString() {
    return javaType + " " + name;
  }
}
