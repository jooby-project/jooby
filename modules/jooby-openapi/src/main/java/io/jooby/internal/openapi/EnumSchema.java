/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.media.StringSchema;

public class EnumSchema extends StringSchema {
  @JsonIgnore private final Map<String, String> fields = new HashMap<>();
  @JsonIgnore private String summary;

  public EnumSchema() {}

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public void setDescription(String name, String description) {
    fields.put(name, description);
  }

  public String getDescription(String name) {
    return fields.get(name);
  }
}
