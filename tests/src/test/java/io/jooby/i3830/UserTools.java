/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import io.jooby.annotation.McpTool;

public class UserTools {

  // The structured output schema we want the LLM to receive
  public record UserProfile(String username, String role, boolean active) {}

  @McpTool(
      name = "get_user_profile",
      description = "Fetches a user profile as a structured JSON object")
  public UserProfile getUserProfile(String username) {
    // In a real app, this would query a database
    return new UserProfile(username, "admin", true);
  }
}
