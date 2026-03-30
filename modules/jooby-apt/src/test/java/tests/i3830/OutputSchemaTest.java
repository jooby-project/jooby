/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class OutputSchemaTest {

  @Test
  public void outputSchemaOff() throws Exception {
    new ProcessorRunner(new OutputSchemaTools())
        .withMcpCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      private io.modelcontextprotocol.spec.McpSchema.Tool schemaOffToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                        var schema = new java.util.LinkedHashMap<String, Object>();
                        schema.put("type", "object");
                        var props = new java.util.LinkedHashMap<String, Object>();
                        schema.put("properties", props);
                        var req = new java.util.ArrayList<String>();
                        schema.put("required", req);
                        return new io.modelcontextprotocol.spec.McpSchema.Tool("schemaOff", null, null, this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), null, null, null);
                      }
                      """);
            });
  }

  @Test
  public void explicitMapOfPojoAsOutputSchema() throws Exception {
    new ProcessorRunner(new OutputSchemaTools())
        .withMcpCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      private io.modelcontextprotocol.spec.McpSchema.Tool schemaMapToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                        var schema = new java.util.LinkedHashMap<String, Object>();
                        schema.put("type", "object");
                        var props = new java.util.LinkedHashMap<String, Object>();
                        schema.put("properties", props);
                        var req = new java.util.ArrayList<String>();
                        schema.put("required", req);
                        java.util.Map<String, Object> schemaMapOutputSchema = null;
                        var schemaMapOutputSchemaNode = schemaGenerator.generateSchema(tests.i3830.Pet.class);
                        var schemaMapOutputSchemaMap = this.json.convertValue(schemaMapOutputSchemaNode, java.util.Map.class);
                        var schemaMapOutputSchemaWrapped = new java.util.LinkedHashMap<String, Object>();
                        schemaMapOutputSchemaWrapped.put("type", "object");
                        schemaMapOutputSchemaWrapped.put("additionalProperties", schemaMapOutputSchemaMap);
                        schemaMapOutputSchema = schemaMapOutputSchemaWrapped;
                        return new io.modelcontextprotocol.spec.McpSchema.Tool("schemaMap", null, null, this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), schemaMapOutputSchema, null, null);
                      }
                      """);
            });
  }

  @Test
  public void explicitListOfPojoAsOutputSchema() throws Exception {
    new ProcessorRunner(new OutputSchemaTools())
        .withMcpCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      private io.modelcontextprotocol.spec.McpSchema.Tool schemaListToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                        var schema = new java.util.LinkedHashMap<String, Object>();
                        schema.put("type", "object");
                        var props = new java.util.LinkedHashMap<String, Object>();
                        schema.put("properties", props);
                        var req = new java.util.ArrayList<String>();
                        schema.put("required", req);
                        java.util.Map<String, Object> schemaListOutputSchema = null;
                        var schemaListOutputSchemaNode = schemaGenerator.generateSchema(tests.i3830.Pet.class);
                        var schemaListOutputSchemaMap = this.json.convertValue(schemaListOutputSchemaNode, java.util.Map.class);
                        var schemaListOutputSchemaWrapped = new java.util.LinkedHashMap<String, Object>();
                        schemaListOutputSchemaWrapped.put("type", "array");
                        schemaListOutputSchemaWrapped.put("items", schemaListOutputSchemaMap);
                        schemaListOutputSchema = schemaListOutputSchemaWrapped;
                        return new io.modelcontextprotocol.spec.McpSchema.Tool("schemaList", null, null, this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), schemaListOutputSchema, null, null);
                      }
                      """);
            });
  }

  @Test
  public void explicitPojoAsOutputSchema() throws Exception {
    new ProcessorRunner(new OutputSchemaTools())
        .withMcpCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      private io.modelcontextprotocol.spec.McpSchema.Tool explicitSchemaToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                        var schema = new java.util.LinkedHashMap<String, Object>();
                        schema.put("type", "object");
                        var props = new java.util.LinkedHashMap<String, Object>();
                        schema.put("properties", props);
                        var req = new java.util.ArrayList<String>();
                        schema.put("required", req);
                        java.util.Map<String, Object> explicitSchemaOutputSchema = null;
                        var explicitSchemaOutputSchemaNode = schemaGenerator.generateSchema(tests.i3830.Pet.class);
                        var explicitSchemaOutputSchemaMap = this.json.convertValue(explicitSchemaOutputSchemaNode, java.util.Map.class);
                        explicitSchemaOutputSchema = explicitSchemaOutputSchemaMap;
                        return new io.modelcontextprotocol.spec.McpSchema.Tool("explicitSchema", null, null, this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), explicitSchemaOutputSchema, null, null);
                      }
                      """);
            });
  }

  @Test
  public void shouldConditionallyGenerateOutputSchema() throws Exception {
    new ProcessorRunner(new OutputSchemaTools())
        .withMcpCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      private io.modelcontextprotocol.spec.McpSchema.Tool defaultSchemaToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                        var schema = new java.util.LinkedHashMap<String, Object>();
                        schema.put("type", "object");
                        var props = new java.util.LinkedHashMap<String, Object>();
                        schema.put("properties", props);
                        var req = new java.util.ArrayList<String>();
                        schema.put("required", req);
                        java.util.Map<String, Object> defaultSchemaOutputSchema = null;
                        if (this.generateOutputSchema) {
                          var defaultSchemaOutputSchemaNode = schemaGenerator.generateSchema(tests.i3830.Pet.class);
                          var defaultSchemaOutputSchemaMap = this.json.convertValue(defaultSchemaOutputSchemaNode, java.util.Map.class);
                          defaultSchemaOutputSchema = defaultSchemaOutputSchemaMap;
                        }
                        return new io.modelcontextprotocol.spec.McpSchema.Tool("defaultSchema", null, null, this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), defaultSchemaOutputSchema, null, null);
                      }
                      """);
            });
  }
}
