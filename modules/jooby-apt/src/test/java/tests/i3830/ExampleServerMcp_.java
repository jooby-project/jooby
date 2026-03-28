/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
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

  public ExampleServerMcp_(
      io.jooby.SneakyThrows.Function<Class<ExampleServer>, ExampleServer> provider) {
    setup(ctx -> provider.apply(ExampleServer.class));
  }

  private void setup(java.util.function.Function<io.jooby.Context, ExampleServer> factory) {
    this.factory = factory;
  }

  private io.modelcontextprotocol.json.McpJsonMapper json;

  @Override
  public void capabilities(
      io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder capabilities) {
    capabilities.tools(true);
    capabilities.prompts(true);
    capabilities.resources(true, true);
    capabilities.completions();
  }

  @Override
  public String serverKey() {
    return "example-server";
  }

  @Override
  public java.util.List<
          io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>
      completions() {
    var completions =
        new java.util.ArrayList<
            io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>();
    completions.add(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(
            new io.modelcontextprotocol.spec.McpSchema.ResourceReference(
                "file:///users/{id}/{name}/profile"),
            (exchange, req) -> this.getUserProfileCompletionHandler(exchange, null, req)));
    completions.add(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(
            new io.modelcontextprotocol.spec.McpSchema.PromptReference("review_code"),
            (exchange, req) -> this.reviewCodeCompletionHandler(exchange, null, req)));
    return completions;
  }

  @Override
  public java.util.List<
          io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>
      statelessCompletions() {
    var completions =
        new java.util.ArrayList<
            io.modelcontextprotocol.server.McpStatelessServerFeatures
                .SyncCompletionSpecification>();
    completions.add(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(
            new io.modelcontextprotocol.spec.McpSchema.ResourceReference(
                "file:///users/{id}/{name}/profile"),
            (ctx, req) -> this.getUserProfileCompletionHandler(null, ctx, req)));
    completions.add(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(
            new io.modelcontextprotocol.spec.McpSchema.PromptReference("review_code"),
            (ctx, req) -> this.reviewCodeCompletionHandler(null, ctx, req)));
    return completions;
  }

  @Override
  public void install(io.jooby.Jooby app, io.modelcontextprotocol.server.McpSyncServer server)
      throws Exception {
    this.json = app.getServices().require(io.modelcontextprotocol.json.McpJsonMapper.class);
    var mapper = app.require(tools.jackson.databind.ObjectMapper.class);
    var configBuilder =
        new com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder(
            com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12,
            com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON);
    var schemaGenerator =
        new com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build());

    server.addTool(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification(
            addToolSpec(mapper, schemaGenerator),
            (exchange, req) -> this.add(exchange, null, req)));
    server.addPrompt(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification(
            reviewCodePromptSpec(), (exchange, req) -> this.reviewCode(exchange, null, req)));
    server.addResource(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification(
            getLogsResourceSpec(), (exchange, req) -> this.getLogs(exchange, null, req)));
    server.addResourceTemplate(
        new io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification(
            getUserProfileResourceTemplateSpec(),
            (exchange, req) -> this.getUserProfile(exchange, null, req)));
  }

  @Override
  public void install(
      io.jooby.Jooby app, io.modelcontextprotocol.server.McpStatelessSyncServer server)
      throws Exception {
    this.json = app.getServices().require(io.modelcontextprotocol.json.McpJsonMapper.class);
    var mapper = app.require(tools.jackson.databind.ObjectMapper.class);
    var configBuilder =
        new com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder(
            com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12,
            com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON);
    var schemaGenerator =
        new com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build());

    server.addTool(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification(
            addToolSpec(mapper, schemaGenerator), (ctx, req) -> this.add(null, ctx, req)));
    server.addPrompt(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification(
            reviewCodePromptSpec(), (ctx, req) -> this.reviewCode(null, ctx, req)));
    server.addResource(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification(
            getLogsResourceSpec(), (ctx, req) -> this.getLogs(null, ctx, req)));
    server.addResourceTemplate(
        new io.modelcontextprotocol.server.McpStatelessServerFeatures
            .SyncResourceTemplateSpecification(
            getUserProfileResourceTemplateSpec(),
            (ctx, req) -> this.getUserProfile(null, ctx, req)));
  }

  private io.modelcontextprotocol.spec.McpSchema.Tool addToolSpec(
      tools.jackson.databind.ObjectMapper mapper,
      com.github.victools.jsonschema.generator.SchemaGenerator schemaGenerator) {
    var schema = mapper.createObjectNode();
    schema.put("type", "object");
    var props = schema.putObject("properties");
    var req = schema.putArray("required");
    var schema_a = schemaGenerator.generateSchema(int.class);
    schema_a.put("description", "1st number");
    props.set("a", schema_a);
    req.add("a");
    var schema_b = schemaGenerator.generateSchema(int.class);
    schema_b.put("description", "2nd number");
    props.set("b", schema_b);
    req.add("b");
    var annotations =
        new io.modelcontextprotocol.spec.McpSchema.ToolAnnotations(
            "Add two numbers.A simple calculator.", true, true, false, true, null);
    return new io.modelcontextprotocol.spec.McpSchema.Tool(
        "calculator",
        "Add two numbers.",
        "A simple calculator.",
        mapper.treeToValue(schema, io.modelcontextprotocol.spec.McpSchema.JsonSchema.class),
        null,
        annotations,
        null);
  }

  private io.modelcontextprotocol.spec.McpSchema.CallToolResult add(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.CallToolRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
    var args =
        req.arguments() != null
            ? req.arguments()
            : java.util.Collections.<String, Object>emptyMap();
    var c = this.factory.apply(ctx);
    var raw_a = args.get("a");
    if (raw_a == null) throw new IllegalArgumentException("Missing req param: a");
    var a =
        raw_a instanceof Number ? ((Number) raw_a).intValue() : Integer.parseInt(raw_a.toString());
    var raw_b = args.get("b");
    if (raw_b == null) throw new IllegalArgumentException("Missing req param: b");
    var b =
        raw_b instanceof Number ? ((Number) raw_b).intValue() : Integer.parseInt(raw_b.toString());
    var result = c.add(a, b);
    return new io.jooby.mcp.McpResult(this.json).toCallToolResult(result, false);
  }

  private io.modelcontextprotocol.spec.McpSchema.Prompt reviewCodePromptSpec() {
    var args = new java.util.ArrayList<io.modelcontextprotocol.spec.McpSchema.PromptArgument>();
    args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("language", null, false));
    args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument("code", null, false));
    return new io.modelcontextprotocol.spec.McpSchema.Prompt(
        "review_code",
        "Review code.",
        "Reviews the given code snippet in the context of the specified programming language.",
        args);
  }

  private io.modelcontextprotocol.spec.McpSchema.GetPromptResult reviewCode(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.GetPromptRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
    var args =
        req.arguments() != null
            ? req.arguments()
            : java.util.Collections.<String, Object>emptyMap();
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
    return new io.modelcontextprotocol.spec.McpSchema.Resource(
        "file:///logs/app.log",
        "Application Logs",
        "Logs Title.",
        "Log description Suspendisse potenti.",
        io.jooby.MediaType.byFileExtension("file:///logs/app.log", "text/plain").getValue(),
        1024L,
        annotations,
        null);
  }

  private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getLogs(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
    var args = java.util.Collections.<String, Object>emptyMap();
    var c = this.factory.apply(ctx);
    var result = c.getLogs();
    return new io.jooby.mcp.McpResult(this.json).toResourceResult(req.uri(), result);
  }

  private io.modelcontextprotocol.spec.McpSchema.ResourceTemplate
      getUserProfileResourceTemplateSpec() {
    return new io.modelcontextprotocol.spec.McpSchema.ResourceTemplate(
        "file:///users/{id}/{name}/profile",
        "getUserProfile",
        "Resource Template.",
        null,
        "application/json",
        null,
        null);
  }

  private io.modelcontextprotocol.spec.McpSchema.ReadResourceResult getUserProfile(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
    var uri = req.uri();
    var manager =
        new io.modelcontextprotocol.util.DefaultMcpUriTemplateManager(
            "file:///users/{id}/{name}/profile");
    var args = new java.util.HashMap<String, Object>();
    args.putAll(manager.extractVariableValues(uri));
    var c = this.factory.apply(ctx);
    var raw_id = args.get("id");
    var id = raw_id != null ? raw_id.toString() : null;
    var raw_name = args.get("name");
    var name = raw_name != null ? raw_name.toString() : null;
    var result = c.getUserProfile(id, name);
    return new io.jooby.mcp.McpResult(this.json).toResourceResult(req.uri(), result);
  }

  private io.modelcontextprotocol.spec.McpSchema.CompleteResult getUserProfileCompletionHandler(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
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

  private io.modelcontextprotocol.spec.McpSchema.CompleteResult reviewCodeCompletionHandler(
      io.modelcontextprotocol.server.McpSyncServerExchange exchange,
      io.modelcontextprotocol.common.McpTransportContext transportContext,
      io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {
    var ctx =
        exchange != null
            ? (io.jooby.Context) exchange.transportContext().get("CTX")
            : (transportContext != null ? (io.jooby.Context) transportContext.get("CTX") : null);
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
