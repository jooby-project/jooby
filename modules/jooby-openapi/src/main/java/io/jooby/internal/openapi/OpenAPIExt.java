/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.Collections;
import java.util.List;

public class OpenAPIExt extends OpenAPI {
  @JsonIgnore
  private List<OperationExt> operations = Collections.emptyList();

  public List<OperationExt> getOperations() {
    return operations;
  }

  public void setOperations(List<OperationExt> operations) {
    this.operations = operations;
  }
}
