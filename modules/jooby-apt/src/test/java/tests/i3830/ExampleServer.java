/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import java.util.List;
import java.util.Map;

import io.jooby.annotation.*;

@McpServer("example-server")
public class ExampleServer {

  // 1. Tool
  @McpTool(name = "calculator", description = "A simple calculator")
  public int add(@McpParam(name = "a") int a, @McpParam(name = "b") int b) {
    return a + b;
  }

  // 2. Prompt
  @McpPrompt(name = "review_code")
  public String reviewCode(
      @McpParam(name = "language") String language, @McpParam(name = "code") String code) {
    return "Please review this " + language + " code:\n" + code;
  }

  // 3. Static Resource
  @McpResource("file:///logs/app.log")
  public String getLogs() {
    return "Log content here...";
  }

  // 4. Resource Template
  @McpResource("file:///users/{id}/profile")
  public Map<String, Object> getUserProfile(@McpParam(name = "id") String id) {
    return Map.of("id", id, "name", "John Doe");
  }

  // 5. Completion (Linked to the Resource Template 'id' argument)
  @McpCompletion(ref = "file:///users/{id}/profile", arg = "id")
  public List<String> completeUserId(String input) {
    // In reality, this might filter a database based on the 'input' prefix
    return List.of("123", "456", "789");
  }
}
