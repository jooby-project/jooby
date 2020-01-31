package io.jooby.internal.openapi;

public class RouteReturnType {
  private String javaType;

  private String overrideJavaType;

  public RouteReturnType(String javaType) {
    this.javaType = javaType;
  }

  public String getJavaType() {
    return javaType;
  }

  public String getOverrideJavaType() {
    return overrideJavaType;
  }

  public void setOverrideJavaType(String overrideJavaType) {
    this.overrideJavaType = overrideJavaType;
  }

  @Override public String toString() {
    return getJavaType();
  }
}
