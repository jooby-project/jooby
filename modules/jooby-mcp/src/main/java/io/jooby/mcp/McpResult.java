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

public class McpResult {

  private final McpJsonMapper json;

  public McpResult(McpJsonMapper json) {
    this.json = json;
  }

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
