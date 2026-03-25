/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import io.jooby.Jooby;
import io.jooby.internal.mcp.MethodInvoker;
import io.jooby.internal.mcp.ToolSpec;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/** Generated Jooby MCP Server. Do not modify manually. */
public class WeatherMcpServer implements JoobyMcpServer {
  private Jooby app;

  private McpJsonMapper mcpJsonMapper;

  /** Map of tool names to its specification. */
  private final Map<String, ToolSpec> tools = new HashMap<>();

  /** Map of tool names to method invokers. */
  private final Map<String, MethodInvoker> toolInvokers = new HashMap<>();

  /** Map of prompt names to its specification. */
  private final Map<String, McpSchema.Prompt> prompts = new HashMap<>();

  /** Map of prompt names to method invokers. */
  private final Map<String, MethodInvoker> promptInvokers = new HashMap<>();

  /** List of completions reference objects. */
  private final List<McpSchema.CompleteReference> completions = new ArrayList<>();

  /** Map of completion key(a composition of <identifier>_<argumentName>) to method invoker. */
  private final Map<String, Function<String, Object>> completionInvokers = new HashMap<>();

  /** List of resources. */
  private final List<McpSchema.Resource> resources = new ArrayList<>();

  /** Map of resource URI to method invoker. */
  private final Map<String, Supplier<Object>> resourceReaders = new HashMap<>();

  /** List of resource templates. */
  private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

  /** Map of resource URI template to method invoker. */
  private final Map<String, Function<Map<String, Object>, Object>> resourceTemplateReaders =
      new HashMap<>();

  /**
   * Initialize a new server.
   *
   * @param app the Jooby application instance
   * @param mcpJsonMapper json serializer instance
   */
  public void init(final Jooby app, final McpJsonMapper mcpJsonMapper) {
    this.app = app;
    this.mcpJsonMapper = mcpJsonMapper;

    tools.put(
        "get_weather",
        ToolSpec.builder()
            .name("get_weather")
            .inputSchema(
                "{\"type\":\"object\",\"properties\":{\"latitude\":{\"type\":\"number\"},\"longitude\":{\"type\":\"number\"}},\"required\":[\"latitude\",\"longitude\"],\"additionalProperties\":false}")
            .requiredArguments(List.of("latitude", "longitude"))
            .build());

    toolInvokers.put(
        "get_weather",
        (args, exchange) ->
            app.require(WeatherServer.class)
                .getWeather((double) args.get("latitude"), (double) args.get("longitude")));
  }

  /**
   * Invokes a tool by name with the provided arguments.
   *
   * @param toolName the name of the tool to invoke
   * @param args the arguments to pass to the tool
   * @return the result of the tool invocation
   */
  public Object invokeTool(
      final String toolName, final Map<String, Object> args, final McpSyncServerExchange exchange) {
    MethodInvoker invoker = toolInvokers.get(toolName);
    return invoker.invoke(args, exchange);
  }

  /**
   * Invokes a prompt by name with the provided arguments.
   *
   * @param promptName the name of the prompt to invoke
   * @param args the arguments to pass to the prompt
   * @return the result of the prompt invocation
   */
  public Object invokePrompt(
      final String promptName,
      final Map<String, Object> args,
      final McpSyncServerExchange exchange) {
    MethodInvoker invoker = promptInvokers.get(promptName);
    return invoker.invoke(args, exchange);
  }

  /**
   * Invokes a completion by identifier(prompt or resource name) and argumentName with the provided
   * argument value.
   *
   * @param identifier prompt or resource template name
   * @param argumentName the name of an argument in prompt or resource template
   * @param input incoming argument value
   * @return the result of the completion invocation
   */
  public Object invokeCompletion(
      final String identifier, final String argumentName, final String input) {
    var completionKey = identifier + '_' + argumentName;
    var invoker = completionInvokers.get(completionKey);
    if (invoker == null) {
      return List.of();
    }
    return invoker.apply(input);
  }

  /**
   * Reads a resource by URI
   *
   * @param uri Resource URI
   * @return resource content
   */
  public Object readResource(final String uri) {
    var reader = resourceReaders.get(uri);
    return reader.get();
  }

  /**
   * Reads a resource by URI according to template
   *
   * @param uriTemplate Resource URI template
   * @return resource content
   */
  public Object readResourceByTemplate(final String uriTemplate, final Map<String, Object> args) {
    var reader = resourceTemplateReaders.get(uriTemplate);
    return reader.apply(args);
  }

  public Map<String, ToolSpec> getTools() {
    return tools;
  }

  public Map<String, McpSchema.Prompt> getPrompts() {
    return prompts;
  }

  public List<McpSchema.CompleteReference> getCompletions() {
    return completions;
  }

  public List<McpSchema.Resource> getResources() {
    return resources;
  }

  public List<McpSchema.ResourceTemplate> getResourceTemplates() {
    return resourceTemplates;
  }

  public String getServerKey() {
    return "weather";
  }
}
