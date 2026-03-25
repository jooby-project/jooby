/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3830;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3830 {
  @Test
  public void shouldGenerateMcpServer() throws Exception {
    new ProcessorRunner(new ExampleServer())
        .withSourceCode(
            source -> {
              assertThat(source)
                  .isEqualToNormalizingWhitespace(
                      """
                      package tests.i3830;

                      @io.jooby.annotation.Generated(ExampleServer.class)
                      public class ExampleServerMcp_ implements io.jooby.mcp.McpService {
                          protected java.util.function.Function<io.jooby.Context, ExampleServer> factory;

                          public ExampleServerMcp_() {
                            this(io.jooby.SneakyThrows.singleton(ExampleServer::new));
                          }

                          public ExampleServerMcp_(ExampleServer instance) {
                             setup(ctx -> instance);
                          }

                          public ExampleServerMcp_(io.jooby.SneakyThrows.Supplier<ExampleServer> provider) {
                             setup(ctx -> (ExampleServer) provider.get());
                          }

                          public ExampleServerMcp_(io.jooby.SneakyThrows.Function<Class<ExampleServer>, ExampleServer> provider) {
                             setup(ctx -> provider.apply(ExampleServer.class));
                          }

                          private void setup(java.util.function.Function<io.jooby.Context, ExampleServer> factory) {
                              this.factory = factory;
                          }

                          private io.modelcontextprotocol.json.McpJsonMapper json;

                          @Override
                          public void capabilities(io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder capabilities) {
                            capabilities.tools(true);
                            capabilities.prompts(true);
                            capabilities.resources(true, true);
                          }

                          @Override
                          public String serverName() {
                            return "example-server";
                          }

                          @Override
                          public java.util.List<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification> completions() {
                            var completions = new java.util.ArrayList<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>();
                            completions.add(new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.ResourceReference("file:///users/{id}/{name}/profile"), this::getUserProfileCompletionHandler));
                            completions.add(new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.PromptReference("review_code"), this::reviewCodeCompletionHandler));
                            return completions;
                          }

                          @Override
                          public void install(io.jooby.Jooby app, io.modelcontextprotocol.server.McpSyncServer server, io.modelcontextprotocol.json.McpJsonMapper json) throws Exception {
                            this.json = json;
                            var mapper = app.require(tools.jackson.databind.ObjectMapper.class);
                            var configBuilder = new com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder(com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12, com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON);
                            var schemaGenerator = new com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build());
                            server.addTool(new io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification(addToolSpec(mapper, schemaGenerator), this::add));

                            server.addPrompt(new io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification(reviewCodePromptSpec(), this::reviewCode));

                            server.addResource(new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification(getLogsResourceSpec(), this::getLogs));

                            server.addResourceTemplate(new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification(getUserProfileResourceTemplateSpec(), this::getUserProfile));

                          }

                          private io.modelcontextprotocol.spec.McpSchema.Tool addToolSpec(tools.jackson.databind.ObjectMapper mapper, com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                            var schema = mapper.createObjectNode();
                            schema.put("type", "object");
                            var props = schema.putObject("properties");
                            var req = schema.putArray("required");
                            props.set("a", schemaGenerator.generateSchema(int.class));
                            req.add("a");
                            props.set("b", schemaGenerator.generateSchema(int.class));
                            req.add("b");
                            return new io.modelcontextprotocol.spec.McpSchema.Tool("calculator", null, "A simple calculator", mapper.treeToValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), null, null, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.CallToolResult add(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.CallToolRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var args = req.arguments() != null ? req.arguments() : java.util.Collections.<String, Object>emptyMap();
                            var c = this.factory.apply(ctx);
                            var raw_a = args.get("a");
                            if (raw_a == null) throw new IllegalArgumentException("Missing req param: a");
                            var a = raw_a instanceof Number ? ((Number) raw_a).intValue() : Integer.parseInt(raw_a.toString());
                            var raw_b = args.get("b");
                            if (raw_b == null) throw new IllegalArgumentException("Missing req param: b");
                            var b = raw_b instanceof Number ? ((Number) raw_b).intValue() : Integer.parseInt(raw_b.toString());
                            var result = c.add(a, b);
                            return new io.jooby.mcp.McpResult(this.json).toCallToolResult(result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.Prompt reviewCodePromptSpec() {
                            var args = new java.util.ArrayList<io.modelcontextprotocol.spec.McpSchema.PromptArgument>();
                            args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("language", null, false));
                            args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("code", null, false));
                            return new io.modelcontextprotocol.spec.McpSchema.Prompt("review_code", null, "", args);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.GetPromptResult reviewCode(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.GetPromptRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var args = req.arguments() != null ? req.arguments() : java.util.Collections.<String, Object>emptyMap();
                            var c = this.factory.apply(ctx);
                            var raw_language = args.get("language");
                            var language = raw_language != null ? raw_language.toString() : null;
                            var raw_code = args.get("code");
                            var code = raw_code != null ? raw_code.toString() : null;
                            var result = c.reviewCode(language, code);
                            return new io.jooby.mcp.McpResult(this.json).toPromptResult(result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.Resource getLogsResourceSpec() {
                            return new io.modelcontextprotocol.spec.McpSchema.Resource("file:///logs/app.log", "getLogs", null, "", null, null, null, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getLogs(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var args = java.util.Collections.<String, Object>emptyMap();
                            var c = this.factory.apply(ctx);
                            var result = c.getLogs();
                            return new io.jooby.mcp.McpResult(this.json).toResourceResult(result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ResourceTemplate getUserProfileResourceTemplateSpec() {
                            return new io.modelcontextprotocol.spec.McpSchema.ResourceTemplate("file:///users/{id}/{name}/profile", "getUserProfile", null, "", null, null, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getUserProfile(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var uri = req.uri();
                            var manager = new io.modelcontextprotocol.util.DefaultMcpUriTemplateManager("file:///users/{id}/{name}/profile");
                            var args = new java.util.HashMap<String, Object>();
                            args.putAll(manager.extractVariableValues(uri));
                            var c = this.factory.apply(ctx);
                            var raw_id = args.get("id");
                            var id = raw_id != null ? raw_id.toString() : null;
                            var result = c.getUserProfile(id);
                            return new io.jooby.mcp.McpResult(this.json).toResourceResult(result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.CompleteResult getUserProfileCompletionHandler(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var c = this.factory.apply(ctx);
                            var targetArg = req.argument() != null ? req.argument().name() : "";
                            var typedValue = req.argument() != null ? req.argument().value() : "";
                            return switch (targetArg) {
                              case "id" -> {
                                var result = c.completeUserId(typedValue);
                                yield new io.jooby.mcp.McpResult(this.json).toCompleteResult(result);
                              }
                              case "name" -> {
                                var result = c.completeUserName(typedValue);
                                yield new io.jooby.mcp.McpResult(this.json).toCompleteResult(result);
                              }
                              default -> new io.jooby.mcp.McpResult(this.json).toCompleteResult(java.util.List.of());
                            };
                          }

                          private io.modelcontextprotocol.spec.McpSchema.CompleteResult reviewCodeCompletionHandler(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {
                            var ctx = (io.jooby.Context) exchange.transportContext().get("CTX");
                            var c = this.factory.apply(ctx);
                            var targetArg = req.argument() != null ? req.argument().name() : "";
                            var typedValue = req.argument() != null ? req.argument().value() : "";
                            return switch (targetArg) {
                              case "language" -> {
                                var result = c.reviewCodelanguage(typedValue);
                                yield new io.jooby.mcp.McpResult(this.json).toCompleteResult(result);
                              }
                              default -> new io.jooby.mcp.McpResult(this.json).toCompleteResult(java.util.List.of());
                            };
                          }
                      }
                      """);
            });
  }
}
