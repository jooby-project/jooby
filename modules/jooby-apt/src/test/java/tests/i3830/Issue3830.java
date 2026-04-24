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
        .withMcpCode(
            source -> {
              assertThat(source)
                  .isEqualToNormalizingNewlines(
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
                            setup(ctx -> provider.get());
                          }

                          public ExampleServerMcp_(io.jooby.SneakyThrows.Function<Class<ExampleServer>, ExampleServer> provider) {
                            setup(ctx -> provider.apply(ExampleServer.class));
                          }

                          private void setup(java.util.function.Function<io.jooby.Context, ExampleServer> factory) {
                            this.factory = factory;
                          }

                          private io.modelcontextprotocol.json.McpJsonMapper json;
                          private boolean generateOutputSchema = false;
                          @Override
                          public void capabilities(io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder capabilities) {
                            capabilities.tools(true);
                            capabilities.prompts(true);
                            capabilities.resources(true, true);
                            capabilities.completions();
                          }

                          @Override
                          public io.jooby.mcp.McpService generateOutputSchema(boolean generateOutputSchema) {
                            this.generateOutputSchema = generateOutputSchema;
                            return this;
                          }

                          @Override
                          public String serverKey() {
                            return "example-server";
                          }

                          @Override
                          public java.util.List<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification> completions(io.jooby.Jooby app) {
                            var invoker = app.require(io.jooby.mcp.McpInvoker.class);
                            var completions = new java.util.ArrayList<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>();
                            completions.add(new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.PromptReference("review_code"), invoker.asCompletionHandler("completions/review_code", "tests.i3830.ExampleServer", "reviewCode", this::reviewCodeCompletionHandler)));
                            completions.add(new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.ResourceReference("file:///users/{id}/{name}/profile"), invoker.asCompletionHandler("completions/file:///users/{id}/{name}/profile", "tests.i3830.ExampleServer", "getUserProfile", this::getUserProfileCompletionHandler)));
                            return completions;
                          }

                          @Override
                          public java.util.List<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification> statelessCompletions(io.jooby.Jooby app) {
                            var invoker = app.require(io.jooby.mcp.McpInvoker.class);
                            var completions = new java.util.ArrayList<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>();
                            completions.add(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.PromptReference("review_code"), invoker.asStatelessCompletionHandler("completions/review_code", "tests.i3830.ExampleServer", "reviewCode", this::reviewCodeCompletionHandler)));
                            completions.add(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(new io.modelcontextprotocol.spec.McpSchema.ResourceReference("file:///users/{id}/{name}/profile"), invoker.asStatelessCompletionHandler("completions/file:///users/{id}/{name}/profile", "tests.i3830.ExampleServer", "getUserProfile", this::getUserProfileCompletionHandler)));
                            return completions;
                          }

                          @Override
                          public void install(io.jooby.Jooby app, io.modelcontextprotocol.server.McpSyncServer server) throws Exception {
                            this.json = app.require(io.modelcontextprotocol.json.McpJsonMapper.class);
                            var invoker = app.require(io.jooby.mcp.McpInvoker.class);
                            var schemaGenerator = app.require(com.github.victools.jsonschema.generator.SchemaGenerator.class);

                            server.addTool(new io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification(addToolSpec(schemaGenerator), invoker.asToolHandler("tools/calculator","tests.i3830.ExampleServer", "add", this::add)));
                            server.addPrompt(new io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification(reviewCodePromptSpec(), invoker.asPromptHandler("prompts/review_code","tests.i3830.ExampleServer", "reviewCode", this::reviewCode)));
                            server.addResource(new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification(getLogsResourceSpec(), invoker.asResourceHandler("resources/file:///logs/app.log","tests.i3830.ExampleServer", "getLogs", this::getLogs)));
                            server.addResourceTemplate(new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification(getUserProfileResourceTemplateSpec(), invoker.asResourceHandler("resources/file:///users/{id}/{name}/profile","tests.i3830.ExampleServer", "getUserProfile", this::getUserProfile)));
                          }

                          @Override
                          public void install(io.jooby.Jooby app, io.modelcontextprotocol.server.McpStatelessSyncServer server) throws Exception {
                            this.json = app.require(io.modelcontextprotocol.json.McpJsonMapper.class);
                            var invoker = app.require(io.jooby.mcp.McpInvoker.class);
                            var schemaGenerator = app.require(com.github.victools.jsonschema.generator.SchemaGenerator.class);

                            server.addTool(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification(addToolSpec(schemaGenerator), invoker.asStatelessToolHandler("tools/calculator","tests.i3830.ExampleServer", "add", this::add)));
                            server.addPrompt(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification(reviewCodePromptSpec(), invoker.asStatelessPromptHandler("prompts/review_code","tests.i3830.ExampleServer", "reviewCode", this::reviewCode)));
                            server.addResource(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification(getLogsResourceSpec(), invoker.asStatelessResourceHandler("resources/file:///logs/app.log","tests.i3830.ExampleServer", "getLogs", this::getLogs)));
                            server.addResourceTemplate(new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification(getUserProfileResourceTemplateSpec(), invoker.asStatelessResourceHandler("resources/file:///users/{id}/{name}/profile","tests.i3830.ExampleServer", "getUserProfile", this::getUserProfile)));
                          }

                          private io.modelcontextprotocol.spec.McpSchema.Tool addToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
                            var schema = new java.util.LinkedHashMap<String, Object>();
                            schema.put("type", "object");
                            var props = new java.util.LinkedHashMap<String, Object>();
                            schema.put("properties", props);
                            var req = new java.util.ArrayList<String>();
                            schema.put("required", req);
                            var schema_a = schemaGenerator.generateSchema(int.class);
                            schema_a.put("description", "1st number");
                            props.put("a", schema_a);
                            req.add("a");
                            var schema_b = schemaGenerator.generateSchema(int.class);
                            schema_b.put("description", "2nd number");
                            props.put("b", schema_b);
                            req.add("b");
                            var annotations = new io.modelcontextprotocol.spec.McpSchema.ToolAnnotations("Add two numbers.A simple calculator.", true, true, false, true, null);
                            return new io.modelcontextprotocol.spec.McpSchema.Tool("calculator", "Add two numbers.", "A simple calculator.", this.json.convertValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), null, annotations, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.CallToolResult add(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.CallToolRequest.class);
                            var args = operation.arguments();
                            var c = this.factory.apply(ctx);
                            var raw_a = args.get("a");
                            if (raw_a == null) throw new IllegalArgumentException("Missing req param: a");
                            var a = ((Number) raw_a).intValue();
                            var raw_b = args.get("b");
                            if (raw_b == null) throw new IllegalArgumentException("Missing req param: b");
                            var b = ((Number) raw_b).intValue();
                            var result = c.add(a, b);
                            return new io.jooby.mcp.McpResult(this.json).toCallToolResult(result, false);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.Prompt reviewCodePromptSpec() {
                            var args = new java.util.ArrayList<io.modelcontextprotocol.spec.McpSchema.PromptArgument>();
                            args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("language", null, false));
                            args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("code", null, false));
                            return new io.modelcontextprotocol.spec.McpSchema.Prompt("review_code", "Review code.", "Reviews the given code snippet in the context of the specified programming language.", args);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.GetPromptResult reviewCode(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.GetPromptRequest.class);
                            var args = operation.arguments();
                            var c = this.factory.apply(ctx);
                            var raw_language = args.get("language");
                            var language = raw_language != null ? raw_language.toString() : null;
                            var raw_code = args.get("code");
                            var code = raw_code != null ? raw_code.toString() : null;
                            var result = c.reviewCode(language, code);
                            return new io.jooby.mcp.McpResult(this.json).toPromptResult(result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.Resource getLogsResourceSpec() {
                            var audience = java.util.List.of(io.modelcontextprotocol.spec.McpSchema.Role.USER);
                            var annotations = new io.modelcontextprotocol.spec.McpSchema.Annotations(audience, 1.5D, "1");
                            return new io.modelcontextprotocol.spec.McpSchema.Resource("file:///logs/app.log", "Application Logs", "Logs Title.", "Log description Suspendisse potenti.", io.jooby.MediaType.byFileExtension("file:///logs/app.log", "text/plain").getValue(), 1024L, annotations, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getLogs(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest.class);
                            var args = java.util.Collections.<String, Object>emptyMap();
                            var c = this.factory.apply(ctx);
                            var result = c.getLogs();
                            return new io.jooby.mcp.McpResult(this.json).toResourceResult(req_.uri(), result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ResourceTemplate getUserProfileResourceTemplateSpec() {
                            return new io.modelcontextprotocol.spec.McpSchema.ResourceTemplate("file:///users/{id}/{name}/profile", "getUserProfile", "Resource Template.", null, "application/json", null, null);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getUserProfile(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest.class);
                            var uri = req_.uri();
                            var manager = new io.modelcontextprotocol.util.DefaultMcpUriTemplateManager("file:///users/{id}/{name}/profile");
                            var args = new java.util.HashMap<String, Object>();
                            args.putAll(manager.extractVariableValues(uri));
                            var c = this.factory.apply(ctx);
                            var raw_id = args.get("id");
                            var id = raw_id != null ? raw_id.toString() : null;
                            var raw_name = args.get("name");
                            var name = raw_name != null ? raw_name.toString() : null;
                            var result = c.getUserProfile(id, name);
                            return new io.jooby.mcp.McpResult(this.json).toResourceResult(req_.uri(), result);
                          }

                          private io.modelcontextprotocol.spec.McpSchema.CompleteResult getUserProfileCompletionHandler(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.CompleteRequest.class);
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var c = this.factory.apply(ctx);
                            var targetArg = req_.argument() != null ? req_.argument().name() : "";
                            var typedValue = req_.argument() != null ? req_.argument().value() : "";
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

                          private io.modelcontextprotocol.spec.McpSchema.CompleteResult reviewCodeCompletionHandler(io.modelcontextprotocol.server.McpSyncServerExchange exchange, io.modelcontextprotocol.common.McpTransportContext transportContext, io.jooby.mcp.McpOperation operation) {
                            var req_ = operation.request(io.modelcontextprotocol.spec.McpSchema.CompleteRequest.class);
                            var ctx = (io.jooby.Context) transportContext.get("CTX");
                            var c = this.factory.apply(ctx);
                            var targetArg = req_.argument() != null ? req_.argument().name() : "";
                            var typedValue = req_.argument() != null ? req_.argument().value() : "";
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
