/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;

public class RequestBodyExt extends RequestBody {

  @JsonIgnore private Object examples;
  @JsonIgnore private String javaType;

  @JsonIgnore private String contentType = MediaType.JSON;

  {
    setRequired(true);
  }

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

  public Object getExamples() {
    return examples;
  }

  public void setExamples(Object examples) {
    this.examples = examples;
  }
}
