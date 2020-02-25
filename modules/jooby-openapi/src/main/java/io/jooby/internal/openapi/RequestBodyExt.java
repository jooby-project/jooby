package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.parameters.RequestBody;

public class RequestBodyExt extends RequestBody {
  @JsonIgnore
  private String javaType;

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }
}
