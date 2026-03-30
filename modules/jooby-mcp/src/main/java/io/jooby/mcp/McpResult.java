/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import io.jooby.SneakyThrows;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Result mapping utility for the Model Context Protocol (MCP) integration.
 *
 * <p>This class acts as the bridge between standard Java/Kotlin return types and the strict
 * JSON-RPC payload structures required by the MCP specification. It is utilized heavily by the
 * APT-generated routing classes ({@code *Mcp_}) to seamlessly translate user-defined method outputs
 * into valid protocol responses.
 *
 * <h2>Conversion Strategies</h2>
 *
 * <ul>
 *   <li><b>Pass-through:</b> If a method returns a native MCP schema object (e.g., {@link
 *       McpSchema.CallToolResult}, {@link McpSchema.GetPromptResult}), it is returned as-is.
 *   <li><b>Primitives & Strings:</b> Standard strings and primitives are automatically wrapped in
 *       the appropriate textual content blocks (e.g., {@link McpSchema.TextContent}).
 *   <li><b>POJOs & Collections:</b> Complex objects and lists are automatically serialized into
 *       JSON strings using the configured {@link McpJsonMapper}, or passed as structured content
 *       depending on the tool's schema capabilities.
 *   <li><b>Prompts:</b> Raw strings or lists returned by a Prompt handler are automatically wrapped
 *       in a {@link McpSchema.PromptMessage} assigned to the {@link McpSchema.Role#USER}.
 * </ul>
 *
 * <p>By handling these conversions internally, developers can write natural, idiomatic Java/Kotlin
 * methods without needing to couple their business logic to the MCP SDK classes.
 *
 * @author edgar
 * @since 4.2.0
 */
public class McpResult {

  private final McpJsonMapper json;

  /**
   * Creates a new result mapper using the provided JSON mapper for object serialization.
   *
   * @param json The JSON mapper instance.
   */
  public McpResult(McpJsonMapper json) {
    this.json = json;
  }

  /**
   * Converts a raw method return value into an MCP tool result.
   *
   * @param result The raw return value from the tool execution.
   * @param structuredContent True if complex objects should be returned as structured data objects,
   *     false to serialize them as JSON text.
   * @return A valid {@link McpSchema.CallToolResult}.
   */
  public McpSchema.CallToolResult toCallToolResult(Object result, boolean structuredContent) {
    try {
      if (result == null) {
        return buildTextResult("null", false);
      } else if (result instanceof McpSchema.CallToolResult callToolResult) {
        return callToolResult;
      } else if (result instanceof String str) {
        return buildTextResult(str, false);
      } else if (result instanceof McpSchema.Content content) {
        return McpSchema.CallToolResult.builder().content(List.of(content)).isError(false).build();
      } else {
        if (structuredContent) {
          return McpSchema.CallToolResult.builder()
              .structuredContent(result)
              .isError(false)
              .build();
        } else {
          return buildTextResult(json.writeValueAsString(result), false);
        }
      }
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Converts a raw method return value into an MCP prompt result.
   *
   * @param result The raw return value from the prompt execution.
   * @return A valid {@link McpSchema.GetPromptResult}.
   */
  public McpSchema.GetPromptResult toPromptResult(Object result) {
    if (result == null) {
      return new McpSchema.GetPromptResult(null, List.of());
    } else if (result instanceof McpSchema.GetPromptResult promptResult) {
      return promptResult;
    } else if (result instanceof McpSchema.PromptMessage promptMessage) {
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof McpSchema.Content content) {
      var promptMessage = new McpSchema.PromptMessage(USER, content);
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof String str) {
      var promptMessage = new McpSchema.PromptMessage(USER, new McpSchema.TextContent(str));
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    } else if (result instanceof List<?> items) {
      //noinspection unchecked
      return handleListReturnType((List<McpSchema.PromptMessage>) result, items);
    } else {
      var promptMessage =
          new McpSchema.PromptMessage(USER, new McpSchema.TextContent(result.toString()));
      return new McpSchema.GetPromptResult(null, List.of(promptMessage));
    }
  }

  /**
   * Converts a raw method return value into an MCP resource result.
   *
   * @param uri The requested resource URI.
   * @param result The raw return value from the resource execution.
   * @return A valid {@link McpSchema.ReadResourceResult}.
   */
  public McpSchema.ReadResourceResult toResourceResult(String uri, Object result) {
    try {
      if (result == null) {
        return new McpSchema.ReadResourceResult(List.of());
      } else if (result instanceof McpSchema.ReadResourceResult resourceResult) {
        return resourceResult;
      } else if (result instanceof McpSchema.ResourceContents resourceContents) {
        return new McpSchema.ReadResourceResult(List.of(resourceContents));
      } else if (result instanceof List<?> contents) {
        return handleListReturnType(result, uri, json, contents);
      } else {
        return toJsonResult(result, uri, json);
      }
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Converts a raw method return value into an MCP completion result.
   *
   * @param result The raw return value from the completion execution.
   * @return A valid {@link McpSchema.CompleteResult}.
   */
  public McpSchema.CompleteResult toCompleteResult(Object result) {
    try {
      Objects.requireNonNull(result, "Completion result cannot be null");

      if (result instanceof McpSchema.CompleteResult completeResult) {
        return completeResult;
      } else if (result instanceof McpSchema.CompleteResult.CompleteCompletion completion) {
        return new McpSchema.CompleteResult(completion);
      } else if (result instanceof List<?> values) {
        if (values.isEmpty()) {
          return new McpSchema.CompleteResult(
              new McpSchema.CompleteResult.CompleteCompletion(List.of(), 0, false));
        } else {
          var item = values.getFirst();
          if (item instanceof String) {
            //noinspection unchecked
            return new McpSchema.CompleteResult(
                new McpSchema.CompleteResult.CompleteCompletion(
                    (List<String>) values, values.size(), false));
          }
        }
      } else if (result instanceof String singleValue) {
        var completion =
            new McpSchema.CompleteResult.CompleteCompletion(List.of(singleValue), 1, false);
        return new McpSchema.CompleteResult(completion);
      }

      throw new IllegalStateException("Unexpected error occurred while handling completion result");
    } catch (Exception ex) {
      throw new McpError(
          new McpSchema.JSONRPCResponse.JSONRPCError(INTERNAL_ERROR, ex.getMessage(), null));
    }
  }

  private static McpSchema.ReadResourceResult handleListReturnType(
      Object result, String uri, McpJsonMapper mcpJsonMapper, List<?> contents) throws IOException {
    if (contents.isEmpty()) {
      return new McpSchema.ReadResourceResult(List.of());
    } else {
      var item = contents.getFirst();
      if (item instanceof McpSchema.ResourceContents) {
        //noinspection unchecked
        return new McpSchema.ReadResourceResult((List<McpSchema.ResourceContents>) contents);
      } else {
        return toJsonResult(result, uri, mcpJsonMapper);
      }
    }
  }

  static McpSchema.ReadResourceResult toJsonResult(
      Object result, String uri, McpJsonMapper mcpJsonMapper) throws IOException {
    var resultStr = mcpJsonMapper.writeValueAsString(result);
    var content = new McpSchema.TextResourceContents(uri, "application/json", resultStr);
    return new McpSchema.ReadResourceResult(List.of(content));
  }

  private McpSchema.CallToolResult buildTextResult(String text, boolean isError) {
    return McpSchema.CallToolResult.builder().addTextContent(text).isError(isError).build();
  }

  private static McpSchema.GetPromptResult handleListReturnType(
      List<McpSchema.PromptMessage> result, List<?> items) {
    if (items.isEmpty()) {
      return new McpSchema.GetPromptResult(null, List.of());
    } else {
      var item = items.getFirst();
      if (item instanceof McpSchema.PromptMessage) {
        return new McpSchema.GetPromptResult(null, result);
      } else {
        var msgs =
            items.stream()
                .map(
                    i -> new McpSchema.PromptMessage(USER, new McpSchema.TextContent(i.toString())))
                .toList();
        return new McpSchema.GetPromptResult(null, msgs);
      }
    }
  }
}
