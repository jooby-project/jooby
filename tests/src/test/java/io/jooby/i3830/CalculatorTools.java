/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import java.util.List;

import io.jooby.annotation.McpCompletion;
import io.jooby.annotation.McpPrompt;
import io.jooby.annotation.McpResource;
import io.jooby.annotation.McpTool;

/** A collection of tools, prompts, and resources exposed to the LLM via MCP. */
public class CalculatorTools {

  // --- TOOLS ---
  @McpTool(name = "add_numbers", description = "Adds two integers together and returns the result.")
  public int add(int a, int b) {
    return a + b;
  }

  // --- PROMPTS ---
  @McpPrompt(name = "math_tutor", description = "A prompt to initiate a math tutoring session")
  public String mathTutor(String topic) {
    return "You are a helpful math tutor. Please explain the concept of "
        + topic
        + " step by step.";
  }

  // --- RESOURCES ---
  @McpResource(
      value = "calculator://manual/usage",
      name = "Calculator Manual",
      description = "Instructions on how to use the calculator")
  public String manual() {
    return "The Calculator supports basic arithmetic. Use the add_numbers tool to sum values.";
  }

  @McpResource(
      value = "calculator://history/{user}",
      name = "User History",
      description = "Retrieves the calculation history for a specific user")
  public String history(String user) {
    return "History for " + user + ":\n5 + 10 = 15\n2 * 4 = 8";
  }

  // --- COMPLETIONS ---
  @McpCompletion(ref = "calculator://history/{user}")
  public List<String> historyCompletions(String user) {
    // In a real app, this would query a database for active usernames matching the input
    return List.of("alice", "bob", "charlie");
  }
}
