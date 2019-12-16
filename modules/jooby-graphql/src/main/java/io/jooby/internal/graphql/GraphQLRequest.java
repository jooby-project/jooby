/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.graphql;

import java.util.Collections;
import java.util.Map;

public class GraphQLRequest {
  private String query;

  private String operationName;

  private Map<String, Object> variables;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public Map<String, Object> getVariables() {
    return variables == null ? Collections.emptyMap() : variables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }
}
