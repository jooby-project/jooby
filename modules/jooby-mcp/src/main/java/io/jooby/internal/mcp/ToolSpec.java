/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public class ToolSpec {
  public static class Builder {
    private ToolSpec spec = new ToolSpec();

    public ToolSpec build() {
      return spec;
    }

    public Builder name(String name) {
      this.spec.setName(name);
      return this;
    }

    public Builder title(String title) {
      this.spec.setTitle(title);
      return this;
    }

    public Builder description(String description) {
      this.spec.setDescription(description);
      return this;
    }

    public Builder inputSchema(String inputSchema) {
      this.spec.setInputSchema(inputSchema);
      return this;
    }

    public Builder outputSchema(String outputSchema) {
      this.spec.setOutputSchema(outputSchema);
      return this;
    }

    public Builder requiredArguments(List<String> requiredArguments) {
      this.spec.setRequiredArguments(requiredArguments);
      return this;
    }
  }

  private String name;
  private String title;
  private String description;
  private String inputSchema;
  private String outputSchema;
  private List<String> requiredArguments;
  private McpSchema.ToolAnnotations annotations;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInputSchema() {
    return inputSchema;
  }

  public void setInputSchema(String inputSchema) {
    this.inputSchema = inputSchema;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public List<String> getRequiredArguments() {
    return requiredArguments;
  }

  public void setRequiredArguments(List<String> requiredArguments) {
    this.requiredArguments = requiredArguments;
  }

  public McpSchema.ToolAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(McpSchema.ToolAnnotations annotations) {
    this.annotations = annotations;
  }

  public static Builder builder() {
    return new Builder();
  }
}
