/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

public class McpResult {

  public McpResult(McpJsonMapper json) {}

  public McpSchema.CallToolResult toCallToolResult(Object result) {
    return null;
  }

  public McpSchema.GetPromptResult toPromptResult(Object result) {
    return null;
  }

  public McpSchema.ReadResourceResult toResourceResult(Object result) {
    return null;
  }

  public McpSchema.CompleteResult toCompleteResult(Object result) {
    return null;
  }
}
