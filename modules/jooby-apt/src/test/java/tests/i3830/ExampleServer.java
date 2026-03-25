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
  /**
   * Reviews the given code snippet in the context of the specified programming language.
   *
   * @param language the programming language of the code to be reviewed
   * @param code the code snippet that needs to be reviewed
   * @return a string containing the prompt to review the provided code
   */
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
  @McpResource("file:///users/{id}/{name}/profile")
  public Map<String, Object> getUserProfile(String id) {
    return Map.of("id", id, "name", "John Doe");
  }

  // 5. Completion (Linked to the Resource Template 'id' argument)
  @McpCompletion(ref = "file:///users/{id}/{name}/profile")
  public List<String> completeUserId(@McpParam(name = "id") String input) {
    return List.of("123", "456", "789");
  }

  // 5. Completion (Linked to the Resource Template 'id' argument)
  @McpCompletion(ref = "file:///users/{id}/{name}/profile")
  public List<String> completeUserName(String name) {
    return List.of("username", "userid");
  }

  @McpCompletion(ref = "review_code")
  public List<String> reviewCodelanguage(String language) {
    return List.of("Java", "Kotlin");
  }
}
