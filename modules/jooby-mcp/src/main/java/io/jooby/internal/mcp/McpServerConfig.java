/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import com.typesafe.config.Config;
import io.jooby.exception.StartupException;
import io.jooby.mcp.McpModule;

/**
 * @author kliushnichenko
 */
public class McpServerConfig {
  public static final String DEFAULT_SSE_ENDPOINT = "/mcp/sse";
  public static final String DEFAULT_MESSAGE_ENDPOINT = "/mcp/message";
  public static final String DEFAULT_MCP_ENDPOINT = "/mcp";

  private String name;
  private String version;
  private McpModule.Transport transport;
  private String sseEndpoint;
  private String messageEndpoint;
  private String mcpEndpoint = DEFAULT_MCP_ENDPOINT;
  private boolean disallowDelete;
  private Integer keepAliveInterval;
  private String instructions;

  public McpServerConfig(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public McpModule.Transport getTransport() {
    return transport;
  }

  public void setTransport(McpModule.Transport transport) {
    this.transport = transport;
  }

  public String getSseEndpoint() {
    return sseEndpoint;
  }

  public void setSseEndpoint(String sseEndpoint) {
    this.sseEndpoint = sseEndpoint;
  }

  public String getMessageEndpoint() {
    return messageEndpoint;
  }

  public void setMessageEndpoint(String messageEndpoint) {
    this.messageEndpoint = messageEndpoint;
  }

  public String getMcpEndpoint() {
    return mcpEndpoint;
  }

  public void setMcpEndpoint(String mcpEndpoint) {
    this.mcpEndpoint = mcpEndpoint;
  }

  public boolean isDisallowDelete() {
    return disallowDelete;
  }

  public void setDisallowDelete(boolean disallowDelete) {
    this.disallowDelete = disallowDelete;
  }

  public Integer getKeepAliveInterval() {
    return keepAliveInterval;
  }

  public void setKeepAliveInterval(Integer keepAliveInterval) {
    this.keepAliveInterval = keepAliveInterval;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public static McpServerConfig fromConfig(String key, Config config) {
    var srvConfig =
        new McpServerConfig(
            resolveRequiredParam(config, "name"), resolveRequiredParam(config, "version"));

    if (config.hasPath("transport")) {
      McpModule.Transport transport = McpModule.Transport.of(config.getString("transport"));
      srvConfig.setTransport(transport);
    } else {
      srvConfig.setTransport(McpModule.Transport.STREAMABLE_HTTP);
    }

    srvConfig.setSseEndpoint(getStrProp("sseEndpoint", DEFAULT_SSE_ENDPOINT, config));
    srvConfig.setMessageEndpoint(getStrProp("messageEndpoint", DEFAULT_MESSAGE_ENDPOINT, config));
    srvConfig.setMcpEndpoint(getStrProp("mcpEndpoint", DEFAULT_MCP_ENDPOINT, config));
    srvConfig.setInstructions(getStrProp("instructions", null, config));
    srvConfig.setDisallowDelete(getBoolProp("disallowDelete", false, config));
    srvConfig.setKeepAliveInterval(getIntProp("keepAliveInterval", null, config));

    return srvConfig;
  }

  public boolean isSseTransport() {
    return this.transport == McpModule.Transport.SSE;
  }

  private static String resolveRequiredParam(Config config, String configPath) {
    if (!config.hasPath(configPath)) {
      throw new StartupException("Missing required config path: " + configPath);
    }
    return config.getString(configPath);
  }

  private static String getStrProp(String propName, String defaultValue, Config config) {
    if (config.hasPath(propName)) {
      return config.getString(propName);
    } else {
      return defaultValue;
    }
  }

  private static boolean getBoolProp(String propName, boolean defaultValue, Config config) {
    if (config.hasPath(propName)) {
      return config.getBoolean(propName);
    } else {
      return defaultValue;
    }
  }

  private static Integer getIntProp(String propName, Integer defaultValue, Config config) {
    if (config.hasPath(propName)) {
      return config.getInt(propName);
    } else {
      return defaultValue;
    }
  }
}
