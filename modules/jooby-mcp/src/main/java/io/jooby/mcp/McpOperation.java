/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.Nullable;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;

/**
 * Contextual information about an MCP (Model Context Protocol) operation being invoked.
 *
 * <p>It acts as a unified data transfer object (DTO) that holds the routing identifier, the target
 * execution method, and the MCP request for any type of MCP request (tools, prompts, resources, or
 * completions).
 *
 * @author edgar
 * @since 4.2.0
 */
public class McpOperation {
  private final String id;
  private final String className;
  private final String methodName;
  private final McpSchema.Request request;
  private final ConcurrentMap<String, Object> arguments;
  private @Nullable Throwable exception;

  private McpOperation(String id, String className, String methodName, McpSchema.Request request) {
    this.id = id;
    this.className = className;
    this.methodName = methodName;
    this.request = request;
    this.arguments = new ConcurrentHashMap<>(arguments(request));
  }

  /**
   * Determines if the current request is an instance of {@code McpSchema.CallToolRequest}.
   *
   * @return {@code true} if the request is a {@code McpSchema.CallToolRequest}, otherwise {@code
   *     false}.
   */
  public boolean isTool() {
    return request instanceof McpSchema.CallToolRequest;
  }

  /**
   * The standard MCP routing identifier (e.g., "tools/add_numbers" or "resources/config.json").
   *
   * @return The standard MCP routing identifier (e.g., "tools/add_numbers" or
   *     "resources/config.json").
   */
  public String getId() {
    return id;
  }

  /**
   * Retrieves the name of the class associated with this operation.
   *
   * @return The name of the target class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Retrieves the name of the method associated with this operation.
   *
   * @return The name of the target method.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Retrieves a map of arguments associated with the current request.
   *
   * <p>Depending on the type of the request, this method performs the following: - If the request
   * is a {@code McpSchema.CallToolRequest}, the arguments from the request are returned. - If the
   * request is a {@code GetPromptRequest}, the arguments from the request are returned. - If the
   * request is a {@code CompleteRequest}, it extracts the name and value of the argument (if
   * present) and returns them as a map. If the argument or its value is missing, an empty map is
   * returned. - For any other request type, an empty map is returned.
   *
   * @return A map containing argument names as keys and their corresponding values. If no arguments
   *     are present, an empty map is returned.
   */
  public Map<String, Object> arguments() {
    return arguments;
  }

  private Map<String, Object> arguments(McpSchema.Request request) {
    return switch (request) {
      case McpSchema.CallToolRequest callToolRequest -> callToolRequest.arguments();
      case GetPromptRequest getPromptRequest -> getPromptRequest.arguments();
      case CompleteRequest completeRequest ->
          completeRequest.argument() != null && completeRequest.argument().value() != null
              ? Map.of(
                  "name",
                  completeRequest.argument().name(),
                  "value",
                  completeRequest.argument().value())
              : Map.of();
      default -> Map.of();
    };
  }

  /**
   * Casts and retrieves the request associated with the current operation as the specified type.
   *
   * @param <R> The type to which the request will be cast.
   * @param type The {@code Class} object representing the type to which the request should be cast.
   *     Must not be null.
   * @return The request cast to the specified type.
   * @throws ClassCastException If the request cannot be cast to the specified type.
   */
  public <R> R request(Class<R> type) {
    return type.cast(request);
  }

  /**
   * Retrieves the request associated with the current operation.
   *
   * @return The {@code McpSchema.Request} object representing the current operation's request.
   */
  public McpSchema.Request getRequest() {
    return request;
  }

  /**
   * Sets an argument for the current operation.
   *
   * @param name The name of the argument to set. Must not be null.
   * @param value The value of the argument to associate with the specified name. Can be null.
   */
  public void setArgument(String name, Object value) {
    this.arguments.put(name, value);
  }

  /**
   * Retrieves the exception associated with the current operation. Internal use only. This
   * exception is set by the default MCP executor in case of an error. It makes sense for a tool
   * error only bc it must generate a tool errored response and the exception is dropped.
   *
   * @return The {@code Throwable} object representing the exception associated with this operation,
   *     or {@code null} if no exception is set.
   */
  public @Nullable Throwable exception() {
    return exception;
  }

  /**
   * Sets the exception associated with this operation. Internal use only.
   *
   * @param exception The {@code Throwable} object representing the exception to set. Can be null.
   */
  public void exception(@Nullable Throwable exception) {
    this.exception = exception;
  }

  /**
   * Creates an operation context for a Tool invocation.
   *
   * @param operationId The standard MCP routing identifier (e.g., "tools/add_numbers").
   * @param targetClass The fully qualified name of the target class.
   * @param targetMethod The name of the target method.
   * @param req The incoming tool request.
   * @return A populated operation context containing the tool name and arguments.
   */
  public static McpOperation create(
      String operationId, String targetClass, String targetMethod, McpSchema.CallToolRequest req) {
    return new McpOperation(operationId, targetClass, targetMethod, req);
  }

  /**
   * Creates an operation context for a Prompt invocation.
   *
   * @param operationId The standard MCP routing identifier (e.g., "prompts/add_numbers").
   * @param targetClass The fully qualified name of the target class.
   * @param targetMethod The name of the target method.
   * @param req The incoming prompt request.
   * @return A populated operation context containing the prompt name and arguments.
   */
  public static McpOperation create(
      String operationId, String targetClass, String targetMethod, GetPromptRequest req) {
    return new McpOperation(operationId, targetClass, targetMethod, req);
  }

  /**
   * Creates an operation context for a Resource read.
   *
   * @param operationId The standard MCP routing identifier (e.g., "resources/config.json").
   * @param targetClass The fully qualified name of the target class.
   * @param targetMethod The name of the target method.
   * @param req The incoming resource request.
   * @return A populated operation context containing the resource URI.
   */
  public static McpOperation create(
      String operationId, String targetClass, String targetMethod, ReadResourceRequest req) {
    return new McpOperation(operationId, targetClass, targetMethod, req);
  }

  /**
   * Creates an operation context for an Autocomplete request.
   *
   * @param operationId The standard MCP routing identifier (e.g., "completions/add_numbers").
   * @param targetClass The fully qualified name of the target class.
   * @param targetMethod The name of the target method.
   * @param req The incoming completion request.
   * @return A populated operation context containing the completion reference and partial argument
   *     values.
   */
  public static McpOperation create(
      String operationId, String targetClass, String targetMethod, CompleteRequest req) {
    return new McpOperation(operationId, targetClass, targetMethod, req);
  }
}
