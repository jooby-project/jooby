/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

public class McpResultTest {

  private McpJsonMapper mapper;
  private McpResult mcpResult;

  @BeforeEach
  void setUp() {
    mapper = mock(McpJsonMapper.class);
    mcpResult = new McpResult(mapper);
  }

  @Test
  void toCallToolResult() throws IOException {
    // Pass-through
    var nativeResult = McpSchema.CallToolResult.builder().addTextContent("hi").build();
    assertSame(nativeResult, mcpResult.toCallToolResult(nativeResult, false));

    // Null
    assertEquals(
        "null",
        ((McpSchema.TextContent) mcpResult.toCallToolResult(null, false).content().get(0)).text());

    // String
    assertEquals(
        "text",
        ((McpSchema.TextContent) mcpResult.toCallToolResult("text", false).content().get(0))
            .text());

    // Content object
    var textContent = new McpSchema.TextContent("raw");
    assertEquals(
        "raw",
        ((McpSchema.TextContent) mcpResult.toCallToolResult(textContent, false).content().get(0))
            .text());

    // POJO - Structured
    Object pojo = Map.of("id", 1);
    var structured = mcpResult.toCallToolResult(pojo, true);
    assertEquals(pojo, structured.structuredContent());

    // POJO - Serialized
    when(mapper.writeValueAsString(pojo)).thenReturn("{\"id\":1}");
    var serialized = mcpResult.toCallToolResult(pojo, false);
    assertEquals("{\"id\":1}", ((McpSchema.TextContent) serialized.content().get(0)).text());

    // Exception handling (SneakyThrows)
    when(mapper.writeValueAsString(any())).thenThrow(new IOException("fail"));
    assertThrows(IOException.class, () -> mcpResult.toCallToolResult(new Object(), false));
  }

  @Test
  void toPromptResult() {
    // Null
    assertTrue(mcpResult.toPromptResult(null).messages().isEmpty());

    // Pass-through native result
    var nativeRes = new McpSchema.GetPromptResult("desc", List.of());
    assertSame(nativeRes, mcpResult.toPromptResult(nativeRes));

    // PromptMessage
    var msg = new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent("hi"));
    assertEquals(
        "hi",
        ((McpSchema.TextContent) mcpResult.toPromptResult(msg).messages().get(0).content()).text());

    // Content
    var content = new McpSchema.TextContent("content");
    assertEquals(
        "content",
        ((McpSchema.TextContent) mcpResult.toPromptResult(content).messages().get(0).content())
            .text());

    // String
    assertEquals(
        "str",
        ((McpSchema.TextContent) mcpResult.toPromptResult("str").messages().get(0).content())
            .text());

    // List of Messages
    var listMsg = List.of(msg);
    assertEquals(1, mcpResult.toPromptResult(listMsg).messages().size());

    // List of Strings (converts to messages)
    var listStr = List.of("a", "b");
    assertEquals(2, mcpResult.toPromptResult(listStr).messages().size());

    // Empty List
    assertTrue(mcpResult.toPromptResult(List.of()).messages().isEmpty());

    // Fallback toString
    assertEquals(
        "123",
        ((McpSchema.TextContent) mcpResult.toPromptResult(123).messages().get(0).content()).text());
  }

  @Test
  void toResourceResult() throws IOException {
    String uri = "mcp://res";

    // Null
    assertTrue(mcpResult.toResourceResult(uri, null).contents().isEmpty());

    // Pass-through ReadResourceResult
    var nativeRes = new McpSchema.ReadResourceResult(List.of());
    assertSame(nativeRes, mcpResult.toResourceResult(uri, nativeRes));

    // ResourceContents
    var content = new McpSchema.TextResourceContents(uri, "text/plain", "data");
    assertEquals(
        "data",
        ((McpSchema.TextResourceContents)
                mcpResult.toResourceResult(uri, content).contents().get(0))
            .text());

    // List - Empty
    assertTrue(mcpResult.toResourceResult(uri, List.of()).contents().isEmpty());

    // List - ResourceContents
    var list = List.of(content);
    assertEquals(1, mcpResult.toResourceResult(uri, list).contents().size());

    // List - Objects (Serialized)
    var pojoList = List.of(Map.of("k", "v"));
    when(mapper.writeValueAsString(pojoList)).thenReturn("[]");
    mcpResult.toResourceResult(uri, pojoList);
    verify(mapper).writeValueAsString(pojoList);

    // Default POJO
    Object pojo = new Object();
    when(mapper.writeValueAsString(pojo)).thenReturn("{}");
    mcpResult.toResourceResult(uri, pojo);
    verify(mapper).writeValueAsString(pojo);

    // Exception
    when(mapper.writeValueAsString(any())).thenThrow(new IOException("fail"));
    assertThrows(IOException.class, () -> mcpResult.toResourceResult(uri, new Object()));
  }

  @Test
  void toCompleteResult() {
    // Null check
    assertThrows(McpError.class, () -> mcpResult.toCompleteResult(null));

    // Pass-through
    var nativeRes =
        new McpSchema.CompleteResult(
            new McpSchema.CompleteResult.CompleteCompletion(List.of("a"), 1, false));
    assertSame(nativeRes, mcpResult.toCompleteResult(nativeRes));

    // CompleteCompletion
    var completion = new McpSchema.CompleteResult.CompleteCompletion(List.of("b"), 1, false);
    assertEquals(1, mcpResult.toCompleteResult(completion).completion().values().size());

    // String
    assertEquals("val", mcpResult.toCompleteResult("val").completion().values().get(0));

    // List - Empty
    assertEquals(0, mcpResult.toCompleteResult(List.of()).completion().values().size());

    // List - Strings
    var list = List.of("x", "y");
    assertEquals(2, mcpResult.toCompleteResult(list).completion().values().size());

    // List - Not Strings (Error Branch)
    assertThrows(McpError.class, () -> mcpResult.toCompleteResult(List.of(123)));

    // Unexpected Object
    assertThrows(McpError.class, () -> mcpResult.toCompleteResult(new Object()));
  }
}
