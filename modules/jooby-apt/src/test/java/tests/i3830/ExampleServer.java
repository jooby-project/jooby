/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import java.util.List;
import java.util.Map;

import io.jooby.annotation.mcp.*;
import io.modelcontextprotocol.spec.McpSchema;

@McpServer("example-server")
public class ExampleServer {

  /**
   * Add two numbers. A simple calculator.
   *
   * @param a 1st number
   * @return sum of the two numbers
   */
  @McpTool(name = "calculator", annotations = @McpTool.McpAnnotations(readOnlyHint = true))
  public int add(@McpParam(name = "a") int a, @McpParam(description = "2nd number") int b) {
    return a + b;
  }

  // 2. Prompt

  /**
   * Review code. Reviews the given code snippet in the context of the specified programming
   * language.
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

  /** Logs Title. Log description Suspendisse potenti. */
  @McpResource(
      uri = "file:///logs/app.log",
      name = "Application Logs",
      size = 1024,
      annotations =
          @McpResource.McpAnnotations(
              audience = McpSchema.Role.USER,
              lastModified = "1",
              priority = 1.5))
  public String getLogs() {
    return "Log content here...";
  }

  /**
   * Resource Template.
   *
   * @param id User ID.
   * @param name User name.
   * @return User profile.
   */
  @McpResource(uri = "file:///users/{id}/{name}/profile", mimeType = "application/json")
  public Map<String, Object> getUserProfile(String id, String name) {
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
