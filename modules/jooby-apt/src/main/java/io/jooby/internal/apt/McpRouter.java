/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.CodeBlock.*;

import java.io.IOException;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class McpRouter extends WebRouter<McpRoute> {

  public McpRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
  }

  public static McpRouter parse(MvcContext context, TypeElement controller) {
    McpRouter router = new McpRouter(context, controller);
    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        McpRoute route = new McpRoute(router, (ExecutableElement) enclosed);
        if (route.isMcpTool()
            || route.isMcpPrompt()
            || route.isMcpResource()
            || route.isMcpResourceTemplate()
            || route.isMcpCompletion()) {
          router.routes.put(route.getMethodName(), route);
        }
      }
    }
    return router;
  }

  @Override
  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString() + "Mcp");
  }

  public String getMcpServerKey() {
    var annotation = AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.McpServer");
    if (annotation != null) {
      return AnnotationSupport.findAnnotationValue(annotation, VALUE).stream()
          .findFirst()
          .orElse("default");
    }
    return "default";
  }

  private String findTargetMethodName(String ref) {
    for (McpRoute route : getRoutes()) {
      if (route.isMcpPrompt()) {
        var annotation =
            AnnotationSupport.findAnnotationByName(
                route.getMethod(), "io.jooby.annotation.McpPrompt");
        String name =
            annotation != null
                ? AnnotationSupport.findAnnotationValue(annotation, "name"::equals).stream()
                    .findFirst()
                    .orElse("")
                : "";
        if (name.isEmpty()) name = route.getMethodName();
        if (ref.equals(name)) return route.getMethodName();
      } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
        var annotation =
            AnnotationSupport.findAnnotationByName(
                route.getMethod(), "io.jooby.annotation.McpResource");
        String uri =
            annotation != null
                ? AnnotationSupport.findAnnotationValue(annotation, "value"::equals).stream()
                    .findFirst()
                    .orElse("")
                : "";
        if (ref.equals(uri)) return route.getMethodName();
      }
    }
    return "mcpTarget" + Math.abs(ref.hashCode());
  }

  @Override
  public String getSourceCode(Boolean generateKotlin) throws IOException {
    if (isEmpty()) return null;

    boolean kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var mcpClassName = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);
    var packageName = getPackageName();

    var template = kt ? KOTLIN : JAVA;
    var buffer = new StringBuilder();

    context.generateStaticImports(
        this,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    var tools = getRoutes().stream().filter(McpRoute::isMcpTool).toList();
    var prompts = getRoutes().stream().filter(McpRoute::isMcpPrompt).toList();
    var resources =
        getRoutes().stream().filter(r -> r.isMcpResource() || r.isMcpResourceTemplate()).toList();

    var completionRoutes = getRoutes().stream().filter(McpRoute::isMcpCompletion).toList();
    java.util.Map<String, java.util.List<McpRoute>> completionGroups =
        new java.util.LinkedHashMap<>();
    for (McpRoute route : completionRoutes) {
      var annotation =
          AnnotationSupport.findAnnotationByName(
              route.getMethod(), "io.jooby.annotation.McpCompletion");
      String ref =
          AnnotationSupport.findAnnotationValue(annotation, "value"::equals).stream()
              .findFirst()
              .orElse("");
      if (ref == null || ref.isEmpty()) {
        ref =
            AnnotationSupport.findAnnotationValue(annotation, "ref"::equals).stream()
                .findFirst()
                .orElse("");
      }
      completionGroups.computeIfAbsent(ref, k -> new java.util.ArrayList<>()).add(route);
    }

    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "private lateinit var json: io.modelcontextprotocol.json.McpJsonMapper\n"));
    } else {
      buffer.append(
          statement(
              indent(4),
              "private io.modelcontextprotocol.json.McpJsonMapper json",
              semicolon(kt),
              "\n"));
    }

    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "override fun capabilities(capabilities:"
                  + " io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder) {"));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public void"
                  + " capabilities(io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.Builder"
                  + " capabilities) {"));
    }
    if (!tools.isEmpty())
      buffer.append(statement(indent(6), "capabilities.tools(true)", semicolon(kt)));
    if (!prompts.isEmpty())
      buffer.append(statement(indent(6), "capabilities.prompts(true)", semicolon(kt)));
    if (!resources.isEmpty())
      buffer.append(statement(indent(6), "capabilities.resources(true, true)", semicolon(kt)));
    buffer.append(statement(indent(4), "}\n"));

    String serverName = getMcpServerKey();
    if (kt) {
      buffer.append(statement(indent(4), "override fun serverName(): String? {"));
      buffer.append(statement(indent(6), "return ", string(serverName)));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(statement(indent(4), "public String serverName() {"));
      buffer.append(statement(indent(6), "return ", string(serverName), semicolon(kt)));
    }
    buffer.append(statement(indent(4), "}\n"));

    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "override fun completions():"
                  + " List<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>"
                  + " {"));
      buffer.append(
          statement(
              indent(6),
              "val completions ="
                  + " mutableListOf<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>()"));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public"
                  + " java.util.List<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>"
                  + " completions() {"));
      buffer.append(
          statement(
              indent(6),
              "var completions = new"
                  + " java.util.ArrayList<io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification>()",
              semicolon(kt)));
    }

    for (String ref : completionGroups.keySet()) {
      boolean isResource = ref.contains("://");
      String handlerName = findTargetMethodName(ref) + "CompletionHandler";
      String refObj =
          isResource
              ? "io.modelcontextprotocol.spec.McpSchema.ResourceReference"
              : "io.modelcontextprotocol.spec.McpSchema.PromptReference";

      if (kt) {
        buffer.append(
            statement(
                indent(6),
                "completions.add(io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(",
                refObj,
                "(",
                string(ref),
                "), this::",
                handlerName,
                "))"));
      } else {
        buffer.append(
            statement(
                indent(6),
                "completions.add(new"
                    + " io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(new"
                    + " ",
                refObj,
                "(",
                string(ref),
                "), this::",
                handlerName,
                "))",
                semicolon(kt)));
      }
    }
    buffer.append(statement(indent(6), "return completions", semicolon(kt)));
    buffer.append(statement(indent(4), "}\n"));

    if (kt) {
      buffer.append(statement(indent(4), "@Throws(Exception::class)"));
      buffer.append(
          statement(
              indent(4),
              "override fun install(app: io.jooby.Jooby, server:"
                  + " io.modelcontextprotocol.server.McpSyncServer, json:"
                  + " io.modelcontextprotocol.json.McpJsonMapper) {"));
      buffer.append(statement(indent(6), "this.json = json"));
      buffer.append(
          statement(
              indent(6),
              "val mapper = app.require(tools.jackson.databind.ObjectMapper::class.java)"));
      if (!tools.isEmpty()) {
        buffer.append(
            statement(
                indent(6),
                "val configBuilder ="
                    + " com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder(com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12,"
                    + " com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON)"));
        buffer.append(
            statement(
                indent(6),
                "val schemaGenerator ="
                    + " com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build())\n"));
      }
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public void install(io.jooby.Jooby app, io.modelcontextprotocol.server.McpSyncServer"
                  + " server, io.modelcontextprotocol.json.McpJsonMapper json) throws Exception"
                  + " {"));
      buffer.append(statement(indent(6), "this.json = json", semicolon(kt)));
      buffer.append(
          statement(
              indent(6),
              "var mapper = app.require(tools.jackson.databind.ObjectMapper.class)",
              semicolon(kt)));
      if (!tools.isEmpty()) {
        buffer.append(
            statement(
                indent(6),
                "var configBuilder = new"
                    + " com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder(com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12,"
                    + " com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON)",
                semicolon(kt)));
        buffer.append(
            statement(
                indent(6),
                "var schemaGenerator = new"
                    + " com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build())",
                semicolon(kt),
                "\n"));
      }
    }

    for (var route :
        getRoutes().stream()
            .filter(
                r ->
                    r.isMcpTool()
                        || r.isMcpPrompt()
                        || r.isMcpResource()
                        || r.isMcpResourceTemplate())
            .toList()) {
      String methodName = route.getMethodName();

      if (route.isMcpTool()) {
        String defArgs = "mapper, schemaGenerator";
        if (kt) {
          buffer.append(
              statement(
                  indent(6),
                  "server.addTool(io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification(",
                  methodName,
                  "ToolSpec(",
                  defArgs,
                  "), this::",
                  methodName,
                  "))\n"));
        } else {
          buffer.append(
              statement(
                  indent(6),
                  "server.addTool(new"
                      + " io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification(",
                  methodName,
                  "ToolSpec(",
                  defArgs,
                  "), this::",
                  methodName,
                  "))",
                  semicolon(kt),
                  "\n"));
        }
      } else if (route.isMcpPrompt()) {
        if (kt) {
          buffer.append(
              statement(
                  indent(6),
                  "server.addPrompt(io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification(",
                  methodName,
                  "PromptSpec(), this::",
                  methodName,
                  "))\n"));
        } else {
          buffer.append(
              statement(
                  indent(6),
                  "server.addPrompt(new"
                      + " io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification(",
                  methodName,
                  "PromptSpec(), this::",
                  methodName,
                  "))",
                  semicolon(kt),
                  "\n"));
        }
      } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
        boolean isTemplate = route.isMcpResourceTemplate();
        String specType =
            isTemplate ? "SyncResourceTemplateSpecification" : "SyncResourceSpecification";
        String addMethod = isTemplate ? "server.addResourceTemplate(" : "server.addResource(";
        String defMethod = isTemplate ? "ResourceTemplateSpec()" : "ResourceSpec()";

        if (kt) {
          buffer.append(
              statement(
                  indent(6),
                  addMethod,
                  "io.modelcontextprotocol.server.McpServerFeatures.",
                  specType,
                  "(",
                  methodName,
                  defMethod,
                  ", this::",
                  methodName,
                  "))\n"));
        } else {
          buffer.append(
              statement(
                  indent(6),
                  addMethod,
                  "new io.modelcontextprotocol.server.McpServerFeatures.",
                  specType,
                  "(",
                  methodName,
                  defMethod,
                  ", this::",
                  methodName,
                  "))",
                  semicolon(kt),
                  "\n"));
        }
      }
    }
    buffer.append(statement(indent(4), "}\n"));

    for (McpRoute route :
        getRoutes().stream()
            .filter(
                r ->
                    r.isMcpTool()
                        || r.isMcpPrompt()
                        || r.isMcpResource()
                        || r.isMcpResourceTemplate())
            .toList()) {
      route.generateMcpDefinitionMethod(kt).forEach(buffer::append);
      route.generateMcpHandlerMethod(kt).forEach(buffer::append);
    }

    for (var entry : completionGroups.entrySet()) {
      String ref = entry.getKey();
      String handlerName = findTargetMethodName(ref) + "CompletionHandler";
      java.util.List<McpRoute> routes = entry.getValue();

      if (kt) {
        buffer.append(
            statement(
                indent(4),
                "private fun ",
                handlerName,
                "(exchange: io.modelcontextprotocol.server.McpSyncServerExchange, req:"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteRequest):"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteResult {"));
        buffer.append(
            statement(
                indent(6),
                "val ctx ="
                    + " exchange.transportContext().get<io.jooby.Context>(io.jooby.Context::class.java.name)"));
        buffer.append(statement(indent(6), "val c = this.factory.apply(ctx)"));
        buffer.append(statement(indent(6), "val targetArg = req.argument()?.name() ?: \"\""));
        buffer.append(statement(indent(6), "val typedValue = req.argument()?.value() ?: \"\""));
        buffer.append(statement(indent(6), "return when (targetArg) {"));
      } else {
        buffer.append(
            statement(
                indent(4),
                "private io.modelcontextprotocol.spec.McpSchema.CompleteResult ",
                handlerName,
                "(io.modelcontextprotocol.server.McpSyncServerExchange exchange,"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {"));
        buffer.append(
            statement(
                indent(6),
                "var ctx = (io.jooby.Context) exchange.transportContext().get(\"CTX\")",
                semicolon(kt)));
        buffer.append(statement(indent(6), "var c = this.factory.apply(ctx)", semicolon(kt)));
        buffer.append(
            statement(
                indent(6),
                "var targetArg = req.argument() != null ? req.argument().name() : \"\"",
                semicolon(kt)));
        buffer.append(
            statement(
                indent(6),
                "var typedValue = req.argument() != null ? req.argument().value() : \"\"",
                semicolon(kt)));
        buffer.append(statement(indent(6), "return switch (targetArg) {"));
      }

      for (McpRoute route : routes) {
        String targetArgName = null;
        java.util.List<String> invokeArgs = new java.util.ArrayList<>();

        for (var param : route.getParameters(false)) {
          String type = param.getType().getRawType().toString();
          if (type.equals("io.jooby.Context")) {
            invokeArgs.add("ctx");
          } else if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")) {
            invokeArgs.add("exchange");
          } else {
            targetArgName = param.getMcpName();
            invokeArgs.add("typedValue");
          }
        }

        if (targetArgName == null) continue;

        if (kt) {
          buffer.append(statement(indent(8), string(targetArgName), " -> {"));
          buffer.append(
              statement(
                  indent(10),
                  "val result = c.",
                  route.getMethodName(),
                  "(",
                  String.join(", ", invokeArgs),
                  ")"));
          buffer.append(
              statement(indent(10), "io.jooby.mcp.McpResult(this.json).toCompleteResult(result)"));
          buffer.append(statement(indent(8), "}"));
        } else {
          buffer.append(statement(indent(8), "case ", string(targetArgName), " -> {"));
          buffer.append(
              statement(
                  indent(10),
                  "var result = c.",
                  route.getMethodName(),
                  "(",
                  String.join(", ", invokeArgs),
                  ")",
                  semicolon(kt)));
          buffer.append(
              statement(
                  indent(10),
                  "yield new io.jooby.mcp.McpResult(this.json).toCompleteResult(result)",
                  semicolon(kt)));
          buffer.append(statement(indent(8), "}"));
        }
      }

      if (kt) {
        buffer.append(
            statement(
                indent(8),
                "else -> io.jooby.mcp.McpResult(this.json).toCompleteResult(emptyList<Any>())"));
        buffer.append(statement(indent(6), "}"));
      } else {
        buffer.append(
            statement(
                indent(8),
                "default -> new"
                    + " io.jooby.mcp.McpResult(this.json).toCompleteResult(java.util.List.of())",
                semicolon(kt)));
        buffer.append(statement(indent(6), "}", semicolon(kt)));
      }
      buffer.append(statement(indent(4), "}\n"));
    }

    return template
        .replace("${packageName}", packageName)
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", mcpClassName)
        .replace("${implements}", "io.jooby.mcp.McpService")
        .replace("${constructors}", constructors(mcpClassName, kt))
        .replace("${methods}", trimr(buffer));
  }
}
