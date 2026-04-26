/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.exception.StartupException;
import io.jooby.mcp.McpModule;

public class McpServerConfigTest {

  @Test
  public void testAccessors() {
    McpServerConfig config = new McpServerConfig("my-server", "1.0");

    config.setName("new-name");
    assertEquals("new-name", config.getName());

    config.setVersion("2.0");
    assertEquals("2.0", config.getVersion());

    config.setTransport(McpModule.Transport.SSE);
    assertEquals(McpModule.Transport.SSE, config.getTransport());
    assertTrue(config.isSseTransport());

    config.setSseEndpoint("/sse");
    assertEquals("/sse", config.getSseEndpoint());

    config.setMessageEndpoint("/msg");
    assertEquals("/msg", config.getMessageEndpoint());

    config.setMcpEndpoint("/mcp-custom");
    assertEquals("/mcp-custom", config.getMcpEndpoint());

    config.setDisallowDelete(true);
    assertTrue(config.isDisallowDelete());

    config.setKeepAliveInterval(60);
    assertEquals(60, config.getKeepAliveInterval());

    config.setInstructions("be helpful");
    assertEquals("be helpful", config.getInstructions());
  }

  @Test
  public void testFromConfigWithDefaults() {
    Config config =
        ConfigFactory.parseMap(
            Map.of(
                "name", "test-server",
                "version", "0.1"));

    McpServerConfig serverConfig = McpServerConfig.fromConfig("mcp", config);

    assertEquals("test-server", serverConfig.getName());
    assertEquals("0.1", serverConfig.getVersion());
    // Default transport
    assertEquals(McpModule.Transport.STREAMABLE_HTTP, serverConfig.getTransport());
    assertFalse(serverConfig.isSseTransport());
    // Default endpoints
    assertEquals(McpServerConfig.DEFAULT_SSE_ENDPOINT, serverConfig.getSseEndpoint());
    assertEquals(McpServerConfig.DEFAULT_MESSAGE_ENDPOINT, serverConfig.getMessageEndpoint());
    assertEquals(McpServerConfig.DEFAULT_MCP_ENDPOINT, serverConfig.getMcpEndpoint());
    // Default booleans/nulls
    assertFalse(serverConfig.isDisallowDelete());
    assertNull(serverConfig.getKeepAliveInterval());
    assertNull(serverConfig.getInstructions());
  }

  @Test
  public void testFromConfigFull() {
    Config config =
        ConfigFactory.parseMap(
            Map.of(
                "name", "full-server",
                "version", "1.0",
                "transport", "sse",
                "sseEndpoint", "/custom/sse",
                "messageEndpoint", "/custom/msg",
                "mcpEndpoint", "/custom/mcp",
                "instructions", "custom instructions",
                "disallowDelete", true,
                "keepAliveInterval", 30));

    McpServerConfig serverConfig = McpServerConfig.fromConfig("mcp", config);

    assertEquals(McpModule.Transport.SSE, serverConfig.getTransport());
    assertTrue(serverConfig.isSseTransport());
    assertEquals("/custom/sse", serverConfig.getSseEndpoint());
    assertEquals("/custom/msg", serverConfig.getMessageEndpoint());
    assertEquals("/custom/mcp", serverConfig.getMcpEndpoint());
    assertEquals("custom instructions", serverConfig.getInstructions());
    assertTrue(serverConfig.isDisallowDelete());
    assertEquals(30, serverConfig.getKeepAliveInterval());
  }

  @Test
  public void testMissingRequiredName() {
    Config config = ConfigFactory.parseMap(Map.of("version", "1.0"));
    assertThrows(StartupException.class, () -> McpServerConfig.fromConfig("mcp", config));
  }

  @Test
  public void testMissingRequiredVersion() {
    Config config = ConfigFactory.parseMap(Map.of("name", "server"));
    assertThrows(StartupException.class, () -> McpServerConfig.fromConfig("mcp", config));
  }
}
