/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import io.jooby.annotation.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;

/** A collection of tools, prompts, and resources exposed to the LLM via MCP. */
public class ArgumentModifier {

  /**
   * Retrieves the username from the provided custom argument.
   *
   * @param user The custom argument containing user information.
   * @return The username extracted from the provided custom argument.
   */
  @McpTool(name = "customArgument")
  public String customizer(CustomArg user, McpSchema.CallToolRequest req) {
    return user.username();
  }
}
