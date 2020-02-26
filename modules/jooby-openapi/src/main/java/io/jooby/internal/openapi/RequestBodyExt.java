package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;

public class RequestBodyExt extends RequestBody {
  @JsonIgnore
  private String javaType;

  @JsonIgnore
  private String contentType = MediaType.JSON;

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
