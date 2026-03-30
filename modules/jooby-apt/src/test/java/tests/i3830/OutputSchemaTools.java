/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import io.jooby.annotation.mcp.McpOutputSchema;
import io.jooby.annotation.mcp.McpTool;

public class OutputSchemaTools {
  @McpTool
  @McpOutputSchema.Off
  public Pet schemaOff() {
    return new Pet("dog");
  }

  @McpTool
  @McpOutputSchema.From(Pet.class)
  public Object explicitSchema() {
    return new Pet("dog");
  }

  @McpTool
  @McpOutputSchema.MapOf(Pet.class)
  public Object schemaMap() {
    return new Pet("dog");
  }

  @McpTool
  @McpOutputSchema.ArrayOf(Pet.class)
  public Object schemaList() {
    return new Pet("dog");
  }

  @McpTool
  public Pet defaultSchema() {
    return new Pet("dog");
  }
}
