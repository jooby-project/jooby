/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.CodeBlock.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import io.jooby.javadoc.JavaDocParser;
import io.jooby.javadoc.MethodDoc;

public class McpRouter extends WebRouter<McpRoute> {

  private final JavaDocParser javadoc;

  public McpRouter(MvcContext context, TypeElement clazz) {
    super(context, clazz);
    var src = Paths.get("").toAbsolutePath();
    if (!src.endsWith("src") && Files.exists(src.resolve("src"))) {
      src = src.resolve("src");
    }
    this.javadoc = new JavaDocParser(src);
  }

  public static McpRouter parse(MvcContext context, TypeElement controller) {
    var router = new McpRouter(context, controller);

    for (var enclosed : controller.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        var route = new McpRoute(router, (ExecutableElement) enclosed);
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
    return context.generateRouterName(getTargetType().getQualifiedName() + "Mcp");
  }

  private String getMcpServerKey() {
    var annotation =
        AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.mcp.McpServer");
    if (annotation != null) {
      return AnnotationSupport.findAnnotationValue(annotation, VALUE).stream()
          .findFirst()
          .orElse("default");
    }
    return "default";
  }

  /**
   * Find completion target must be a prompt or resource.
   *
   * @param ref Prompt name or resource uri.
   * @return Method name.
   */
  private String findTargetMethodName(String ref) {
    for (var route : getRoutes()) {
      if (route.isMcpPrompt()) {
        var annotation =
            AnnotationSupport.findAnnotationByName(
                route.getMethod(), "io.jooby.annotation.mcp.McpPrompt");
        var name =
            annotation != null
                ? AnnotationSupport.findAnnotationValue(annotation, "name"::equals).stream()
                    .findFirst()
                    .orElse("")
                : "";
        if (name.isEmpty()) {
          name = route.getMethodName();
        }
        if (ref.equals(name)) {
          return route.getMethodName();
        }
      } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
        var annotation =
            AnnotationSupport.findAnnotationByName(
                route.getMethod(), "io.jooby.annotation.mcp.McpResource");
        var uri =
            annotation != null
                ? AnnotationSupport.findAnnotationValue(annotation, "uri"::equals).stream()
                    .findFirst()
                    .orElse("")
                : "";
        if (ref.equals(uri)) {
          return route.getMethodName();
        }
      }
    }
    return "mcpTarget" + Math.abs(ref.hashCode());
  }

  @Override
  public String toSourceCode(boolean kt) throws IOException {
    var generateTypeName = getTargetType().getSimpleName().toString();
    var mcpClassName = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);
    var packageName = getPackageName();

    var template = getTemplate(kt);
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
    var completionGroups = new java.util.LinkedHashMap<String, java.util.List<McpRoute>>();
    for (var route : completionRoutes) {
      var annotation =
          AnnotationSupport.findAnnotationByName(
              route.getMethod(), "io.jooby.annotation.mcp.McpCompletion");
      String ref =
          AnnotationSupport.findAnnotationValue(annotation, "value"::equals).stream()
              .findFirst()
              .orElse(null);
      if (ref == null || ref.isEmpty()) {
        ref =
            AnnotationSupport.findAnnotationValue(annotation, "ref"::equals).stream()
                .findFirst()
                .orElse("");
      }
      completionGroups.computeIfAbsent(ref, k -> new ArrayList<>()).add(route);
    }

    if (kt) {
      buffer.append(
          statement(
              indent(4), "private lateinit var json: io.modelcontextprotocol.json.McpJsonMapper"));
    } else {
      buffer.append(
          statement(
              indent(4), "private io.modelcontextprotocol.json.McpJsonMapper json", semicolon(kt)));
    }

    // --- capabilities() ---
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
    if (!tools.isEmpty()) {
      buffer.append(statement(indent(6), "capabilities.tools(true)", semicolon(kt)));
    }
    if (!prompts.isEmpty()) {
      buffer.append(statement(indent(6), "capabilities.prompts(true)", semicolon(kt)));
    }
    if (!resources.isEmpty()) {
      buffer.append(statement(indent(6), "capabilities.resources(true, true)", semicolon(kt)));
    }
    if (!completionGroups.isEmpty()) {
      buffer.append(statement(indent(6), "capabilities.completions()", semicolon(kt)));
    }
    buffer.append(statement(indent(4), "}\n"));

    // --- serverKey() ---
    var serverName = getMcpServerKey();
    if (kt) {
      buffer.append(statement(indent(4), "override fun serverKey(): String {"));
      buffer.append(statement(indent(6), "return ", string(serverName)));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(statement(indent(4), "public String serverKey() {"));
      buffer.append(statement(indent(6), "return ", string(serverName), semicolon(kt)));
    }
    buffer.append(statement(indent(4), "}\n"));

    // --- completions() ---
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

    for (var ref : completionGroups.keySet()) {
      var isResource = ref.contains("://");
      var handlerName = findTargetMethodName(ref) + "CompletionHandler";
      var refObj =
          isResource
              ? "io.modelcontextprotocol.spec.McpSchema.ResourceReference"
              : "io.modelcontextprotocol.spec.McpSchema.PromptReference";

      String lambda =
          kt
              ? "{ exchange, req -> this." + handlerName + "(exchange, null, req) }"
              : "(exchange, req) -> this." + handlerName + "(exchange, null, req)";

      if (kt) {
        buffer.append(
            statement(
                indent(6),
                "completions.add(io.modelcontextprotocol.server.McpServerFeatures.SyncCompletionSpecification(",
                refObj,
                "(",
                string(ref),
                "), ",
                lambda,
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
                "), ",
                lambda,
                "))",
                semicolon(kt)));
      }
    }
    buffer.append(statement(indent(6), "return completions", semicolon(kt)));
    buffer.append(statement(indent(4), "}\n"));

    // --- statelessCompletions() ---
    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "override fun statelessCompletions():"
                  + " List<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>"
                  + " {"));
      buffer.append(
          statement(
              indent(6),
              "val completions ="
                  + " mutableListOf<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>()"));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public"
                  + " java.util.List<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>"
                  + " statelessCompletions() {"));
      buffer.append(
          statement(
              indent(6),
              "var completions = new"
                  + " java.util.ArrayList<io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification>()",
              semicolon(kt)));
    }

    for (var ref : completionGroups.keySet()) {
      var isResource = ref.contains("://");
      var handlerName = findTargetMethodName(ref) + "CompletionHandler";
      var refObj =
          isResource
              ? "io.modelcontextprotocol.spec.McpSchema.ResourceReference"
              : "io.modelcontextprotocol.spec.McpSchema.PromptReference";

      String lambda =
          kt
              ? "{ ctx, req -> this." + handlerName + "(null, ctx, req) }"
              : "(ctx, req) -> this." + handlerName + "(null, ctx, req)";

      if (kt) {
        buffer.append(
            statement(
                indent(6),
                "completions.add(io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(",
                refObj,
                "(",
                string(ref),
                "), ",
                lambda,
                "))"));
      } else {
        buffer.append(
            statement(
                indent(6),
                "completions.add(new"
                    + " io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification(new"
                    + " ",
                refObj,
                "(",
                string(ref),
                "), ",
                lambda,
                "))",
                semicolon(kt)));
      }
    }
    buffer.append(statement(indent(6), "return completions", semicolon(kt)));
    buffer.append(statement(indent(4), "}\n"));

    // --- install() methods ---
    String[] serverTypes = {
      "io.modelcontextprotocol.server.McpSyncServer",
      "io.modelcontextprotocol.server.McpStatelessSyncServer"
    };

    for (String serverType : serverTypes) {
      if (kt) {
        buffer.append(statement(indent(4), "@Throws(Exception::class)"));
        buffer.append(
            statement(
                indent(4),
                "override fun install(app: io.jooby.Jooby, server: " + serverType + ") {"));
        buffer.append(
            statement(
                indent(6),
                "this.json ="
                    + " app.require(io.modelcontextprotocol.json.McpJsonMapper::class.java)"));

        if (!tools.isEmpty()) {
          buffer.append(
              statement(
                  indent(6),
                  "val schemaGenerator ="
                      + " app.require(com.github.victools.jsonschema.generator.SchemaGenerator::class.java)"));
        }
      } else {
        buffer.append(statement(indent(4), "@Override"));
        buffer.append(
            statement(
                indent(4),
                "public void install(io.jooby.Jooby app, "
                    + serverType
                    + " server) throws Exception {"));
        buffer.append(
            statement(
                indent(6),
                "this.json =" + " app.require(io.modelcontextprotocol.json.McpJsonMapper.class)",
                semicolon(kt)));

        if (!tools.isEmpty()) {
          buffer.append(
              statement(
                  indent(6),
                  "var schemaGenerator ="
                      + " app.require(com.github.victools.jsonschema.generator.SchemaGenerator.class)",
                  semicolon(kt)));
        }
      }

      buffer.append(System.lineSeparator());

      boolean isStateless = serverType.contains("Stateless");
      String featuresClass = isStateless ? "McpStatelessServerFeatures" : "McpServerFeatures";

      for (var route : getRoutes()) {
        var methodName = route.getMethodName();

        // --- Lambda Router Definition ---
        String lambda =
            kt
                ? (isStateless
                    ? "{ ctx, req -> this." + methodName + "(null, ctx, req) }"
                    : "{ exchange, req -> this." + methodName + "(exchange, null, req) }")
                : (isStateless
                    ? "(ctx, req) -> this." + methodName + "(null, ctx, req)"
                    : "(exchange, req) -> this." + methodName + "(exchange, null, req)");

        if (route.isMcpTool()) {
          // Removed "mapper" from defArgs
          var defArgs = "schemaGenerator";
          if (kt) {
            buffer.append(
                statement(
                    indent(6),
                    "server.addTool(io.modelcontextprotocol.server.",
                    featuresClass,
                    ".SyncToolSpecification(",
                    methodName,
                    "ToolSpec(",
                    defArgs,
                    "), ",
                    lambda,
                    "))"));
          } else {
            buffer.append(
                statement(
                    indent(6),
                    "server.addTool(new io.modelcontextprotocol.server.",
                    featuresClass,
                    ".SyncToolSpecification(",
                    methodName,
                    "ToolSpec(",
                    defArgs,
                    "), ",
                    lambda,
                    "));"));
          }
        } else if (route.isMcpPrompt()) {
          if (kt) {
            buffer.append(
                statement(
                    indent(6),
                    "server.addPrompt(io.modelcontextprotocol.server.",
                    featuresClass,
                    ".SyncPromptSpecification(",
                    methodName,
                    "PromptSpec(), ",
                    lambda,
                    "))"));
          } else {
            buffer.append(
                statement(
                    indent(6),
                    "server.addPrompt(new io.modelcontextprotocol.server.",
                    featuresClass,
                    ".SyncPromptSpecification(",
                    methodName,
                    "PromptSpec(), ",
                    lambda,
                    "));"));
          }
        } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
          var isTemplate = route.isMcpResourceTemplate();
          var specType =
              isTemplate ? "SyncResourceTemplateSpecification" : "SyncResourceSpecification";
          var addMethod = isTemplate ? "server.addResourceTemplate(" : "server.addResource(";
          var defMethod = isTemplate ? "ResourceTemplateSpec()" : "ResourceSpec()";

          if (kt) {
            buffer.append(
                statement(
                    indent(6),
                    addMethod,
                    "io.modelcontextprotocol.server.",
                    featuresClass,
                    ".",
                    specType,
                    "(",
                    methodName,
                    defMethod,
                    ", ",
                    lambda,
                    "))"));
          } else {
            buffer.append(
                statement(
                    indent(6),
                    addMethod,
                    "new io.modelcontextprotocol.server.",
                    featuresClass,
                    ".",
                    specType,
                    "(",
                    methodName,
                    defMethod,
                    ", ",
                    lambda,
                    "));"));
          }
        }
      }
      buffer.append(statement(indent(4), "}", System.lineSeparator()));
    }

    for (var route : getRoutes()) {
      route.generateMcpDefinitionMethod(kt).forEach(buffer::append);
      route.generateMcpHandlerMethod(kt).forEach(buffer::append);
    }

    // --- Generate Unified Completion Handlers ---
    for (var entry : completionGroups.entrySet()) {
      var ref = entry.getKey();
      var handlerName = findTargetMethodName(ref) + "CompletionHandler";
      var routes = entry.getValue();

      if (kt) {
        buffer.append(
            statement(
                indent(4),
                "private fun ",
                handlerName,
                "(exchange: io.modelcontextprotocol.server.McpSyncServerExchange?,"
                    + " transportContext: io.modelcontextprotocol.common.McpTransportContext?, req:"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteRequest):"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteResult {"));
        buffer.append(
            statement(
                indent(6),
                "val ctx ="
                    + " exchange?.transportContext()?.get<io.jooby.Context>(io.jooby.Context::class.java.name)"
                    + " ?: transportContext?.get<io.jooby.Context>(io.jooby.Context::class.java.name)"));
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
                    + " io.modelcontextprotocol.common.McpTransportContext transportContext,"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteRequest req) {"));
        buffer.append(
            statement(
                indent(6),
                "var ctx = exchange != null ? (io.jooby.Context)"
                    + " exchange.transportContext().get(\"CTX\") : (transportContext != null ?"
                    + " (io.jooby.Context) transportContext.get(\"CTX\") : null)",
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

      for (var route : routes) {
        String targetArgName = null;
        var invokeArgs = new java.util.ArrayList<String>();

        for (var param : route.getParameters(true)) {
          var type = param.getType().getRawType().toString();
          if (type.equals("io.jooby.Context")) {
            invokeArgs.add("ctx");
          } else if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")) {
            invokeArgs.add("exchange");
          } else if (type.equals("io.modelcontextprotocol.common.McpTransportContext")) {
            if (kt) {
              invokeArgs.add("exchange?.transportContext() ?: transportContext");
            } else {
              invokeArgs.add("exchange != null ? exchange.transportContext() : transportContext");
            }
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
      buffer.append(statement(indent(4), "}", System.lineSeparator()));
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

  public Optional<MethodDoc> getMethodDoc(String methodName, List<String> types) {
    return javadoc.parse(getTargetType().toString()).flatMap(it -> it.getMethod(methodName, types));
  }
}
