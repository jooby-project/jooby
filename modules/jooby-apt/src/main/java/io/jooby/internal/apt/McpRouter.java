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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

  private String findTargetMethodName(String ref) {
    for (var route : getRoutes()) {
      if ((route.isMcpPrompt() || route.isMcpResource() || route.isMcpResourceTemplate())
          && ref.equals(getMcpRouteName(route))) {
        return route.getMethodName();
      }
    }
    return "mcpTarget" + Math.abs(ref.hashCode());
  }

  private String getMcpRouteName(McpRoute route) {
    String annotationName = null;
    String attrName = "name";

    if (route.isMcpTool()) {
      annotationName = "io.jooby.annotation.mcp.McpTool";
    } else if (route.isMcpPrompt()) {
      annotationName = "io.jooby.annotation.mcp.McpPrompt";
    } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
      annotationName = "io.jooby.annotation.mcp.McpResource";
      attrName = "uri";
    }

    if (annotationName != null) {
      var ann = AnnotationSupport.findAnnotationByName(route.getMethod(), annotationName);
      if (ann != null) {
        final String finalAttrName = attrName;
        var name =
            AnnotationSupport.findAnnotationValue(ann, finalAttrName::equals).stream()
                .findFirst()
                .orElse("");
        if (!name.isEmpty()) return name;
      }
    }
    return route.getMethodName();
  }

  private String getMcpRouteType(McpRoute route) {
    if (route.isMcpTool()) return "tools";
    if (route.isMcpPrompt()) return "prompts";
    if (route.isMcpResource() || route.isMcpResourceTemplate()) return "resources";
    return "";
  }

  @Override
  public String toSourceCode(boolean kt) throws IOException {
    var generateTypeName = getTargetType().getSimpleName().toString();
    var mcpClassName = getGeneratedType().substring(getGeneratedType().lastIndexOf('.') + 1);

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

    var completionGroups = buildCompletionGroups();
    var allCompletionRefs = buildAllCompletionRefs(prompts, resources, completionGroups);

    appendFields(buffer, kt);
    appendCapabilities(buffer, kt, tools, prompts, resources, completionGroups);
    appendGenerateOutputSchema(buffer, kt);
    appendServerKey(buffer, kt);

    // Generate both stateful and stateless completion methods
    appendCompletions(buffer, kt, false, allCompletionRefs, completionGroups);
    appendCompletions(buffer, kt, true, allCompletionRefs, completionGroups);

    // Generate both stateful and stateless install methods
    appendInstall(buffer, kt, false, tools);
    appendInstall(buffer, kt, true, tools);

    // Append handler methods
    for (var route : getRoutes()) {
      route.generateMcpDefinitionMethod(kt).forEach(buffer::append);
      route.generateMcpHandlerMethod(kt).forEach(buffer::append);
    }
    appendCompletionHandlers(buffer, kt, completionGroups);

    return getTemplate(kt)
        .replace("${packageName}", getPackageName())
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", mcpClassName)
        .replace("${implements}", "io.jooby.mcp.McpService")
        .replace("${constructors}", constructors(mcpClassName, kt))
        .replace("${methods}", trimr(buffer));
  }

  private Map<String, List<McpRoute>> buildCompletionGroups() {
    var groups = new LinkedHashMap<String, List<McpRoute>>();
    for (var route : getRoutes().stream().filter(McpRoute::isMcpCompletion).toList()) {
      var ann =
          AnnotationSupport.findAnnotationByName(
              route.getMethod(), "io.jooby.annotation.mcp.McpCompletion");
      String ref =
          AnnotationSupport.findAnnotationValue(ann, "value"::equals).stream()
              .findFirst()
              .orElse(null);
      if (ref == null || ref.isEmpty()) {
        ref =
            AnnotationSupport.findAnnotationValue(ann, "ref"::equals).stream()
                .findFirst()
                .orElse("");
      }
      groups.computeIfAbsent(ref, k -> new ArrayList<>()).add(route);
    }
    return groups;
  }

  private Set<String> buildAllCompletionRefs(
      List<McpRoute> prompts,
      List<McpRoute> resources,
      Map<String, List<McpRoute>> completionGroups) {
    var refs = new LinkedHashSet<String>();
    prompts.forEach(r -> refs.add(getMcpRouteName(r)));
    resources.stream()
        .filter(McpRoute::isMcpResourceTemplate)
        .forEach(r -> refs.add(getMcpRouteName(r)));
    refs.addAll(completionGroups.keySet());
    return refs;
  }

  private void appendFields(StringBuilder buffer, boolean kt) {
    if (kt) {
      buffer.append(
          statement(
              indent(4), "private lateinit var json: io.modelcontextprotocol.json.McpJsonMapper"));
      buffer.append(statement(indent(4), "private var generateOutputSchema: Boolean = false"));
    } else {
      buffer.append(
          statement(
              indent(4), "private io.modelcontextprotocol.json.McpJsonMapper json", semicolon(kt)));
      buffer.append(
          statement(indent(4), "private boolean generateOutputSchema = false", semicolon(kt)));
    }
  }

  private void appendCapabilities(
      StringBuilder buffer,
      boolean kt,
      List<McpRoute> tools,
      List<McpRoute> prompts,
      List<McpRoute> resources,
      Map<?, ?> completions) {
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
    if (!completions.isEmpty())
      buffer.append(statement(indent(6), "capabilities.completions()", semicolon(kt)));

    buffer.append(statement(indent(4), "}\n"));
  }

  private void appendGenerateOutputSchema(StringBuilder buffer, boolean kt) {
    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "override fun generateOutputSchema(generateOutputSchema: Boolean):"
                  + " io.jooby.mcp.McpService {"));
      buffer.append(statement(indent(6), "this.generateOutputSchema = generateOutputSchema"));
      buffer.append(statement(indent(6), "return this"));
      buffer.append(statement(indent(4), "}\n"));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public io.jooby.mcp.McpService generateOutputSchema(boolean generateOutputSchema)"
                  + " {"));
      buffer.append(
          statement(indent(6), "this.generateOutputSchema = generateOutputSchema", semicolon(kt)));
      buffer.append(statement(indent(6), "return this", semicolon(kt)));
      buffer.append(statement(indent(4), "}\n"));
    }
  }

  private void appendServerKey(StringBuilder buffer, boolean kt) {
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
  }

  private void appendCompletions(
      StringBuilder buffer,
      boolean kt,
      boolean isStateless,
      Set<String> allRefs,
      Map<String, List<McpRoute>> groups) {
    String methodName = isStateless ? "statelessCompletions" : "completions";
    String featureClass = isStateless ? "McpStatelessServerFeatures" : "McpServerFeatures";

    if (kt) {
      buffer.append(
          statement(
              indent(4),
              "override fun ",
              methodName,
              "(app: io.jooby.Jooby): List<io.modelcontextprotocol.server.",
              featureClass,
              ".SyncCompletionSpecification> {"));
      buffer.append(
          statement(indent(6), "val invoker = app.require(io.jooby.mcp.McpInvoker::class.java)"));
      buffer.append(
          statement(
              indent(6),
              "val completions = mutableListOf<io.modelcontextprotocol.server.",
              featureClass,
              ".SyncCompletionSpecification>()"));
    } else {
      buffer.append(statement(indent(4), "@Override"));
      buffer.append(
          statement(
              indent(4),
              "public java.util.List<io.modelcontextprotocol.server.",
              featureClass,
              ".SyncCompletionSpecification> ",
              methodName,
              "(io.jooby.Jooby app) {"));
      buffer.append(
          statement(
              indent(6),
              "var invoker = app.require(io.jooby.mcp.McpInvoker.class)",
              semicolon(kt)));
      buffer.append(
          statement(
              indent(6),
              "var completions = new java.util.ArrayList<io.modelcontextprotocol.server.",
              featureClass,
              ".SyncCompletionSpecification>()",
              semicolon(kt)));
    }

    for (var ref : allRefs) {
      var isResource = ref.contains("://");
      var refObj =
          isResource
              ? "io.modelcontextprotocol.spec.McpSchema.ResourceReference"
              : "io.modelcontextprotocol.spec.McpSchema.PromptReference";

      String lambda;
      if (groups.containsKey(ref)) {
        var targetMethod = findTargetMethodName(ref);
        var handlerName = targetMethod + "CompletionHandler";
        var operationArg = generateOperationArg(kt, "completions/" + ref, targetMethod);

        String invokeArgs =
            isStateless ? "null, ctx, req" : "exchange, exchange.transportContext(), req";
        String lambdaArgs = isStateless ? "ctx, req" : "exchange, req";

        lambda =
            kt
                ? "{ "
                    + lambdaArgs
                    + " -> invoker.invoke("
                    + operationArg
                    + ") { this."
                    + handlerName
                    + "("
                    + invokeArgs
                    + ") } }"
                : "("
                    + lambdaArgs
                    + ") -> invoker.invoke("
                    + operationArg
                    + ", () -> this."
                    + handlerName
                    + "("
                    + invokeArgs
                    + "))";
      } else {
        lambda =
            kt
                ? "{ _, _ -> io.jooby.mcp.McpResult(this.json).toCompleteResult(emptyList<Any>()) }"
                : "(exchange, req) -> new"
                    + " io.jooby.mcp.McpResult(this.json).toCompleteResult(java.util.List.of())";
      }

      String specification =
          "io.modelcontextprotocol.server." + featureClass + ".SyncCompletionSpecification";
      if (kt) {
        buffer.append(
            statement(
                indent(6),
                "completions.add(",
                specification,
                "(",
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
                "completions.add(new ",
                specification,
                "(new ",
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
  }

  private void appendInstall(
      StringBuilder buffer, boolean kt, boolean isStateless, List<McpRoute> tools) {
    String serverType =
        isStateless
            ? "io.modelcontextprotocol.server.McpStatelessSyncServer"
            : "io.modelcontextprotocol.server.McpSyncServer";
    String featuresClass = isStateless ? "McpStatelessServerFeatures" : "McpServerFeatures";

    if (kt) {
      buffer.append(statement(indent(4), "@Throws(Exception::class)"));
      buffer.append(
          statement(
              indent(4), "override fun install(app: io.jooby.Jooby, server: ", serverType, ") {"));
      buffer.append(
          statement(
              indent(6),
              "this.json = app.require(io.modelcontextprotocol.json.McpJsonMapper::class.java)"));
      buffer.append(
          statement(indent(6), "val invoker = app.require(io.jooby.mcp.McpInvoker::class.java)"));
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
              "public void install(io.jooby.Jooby app, ",
              serverType,
              " server) throws Exception {"));
      buffer.append(
          statement(
              indent(6),
              "this.json = app.require(io.modelcontextprotocol.json.McpJsonMapper.class)",
              semicolon(kt)));
      buffer.append(
          statement(
              indent(6),
              "var invoker = app.require(io.jooby.mcp.McpInvoker.class)",
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

    for (var route : getRoutes()) {
      var methodName = route.getMethodName();
      var mcpName = getMcpRouteName(route);
      var mcpType = getMcpRouteType(route);
      if (mcpType.isEmpty()) continue;

      var operationArg = generateOperationArg(kt, mcpType + "/" + mcpName, methodName);

      String invokeArgs =
          isStateless ? "null, ctx, req" : "exchange, exchange.transportContext(), req";
      String lambdaArgs = isStateless ? "ctx, req" : "exchange, req";

      var lambda =
          kt
              ? "{ "
                  + lambdaArgs
                  + " -> invoker.invoke("
                  + operationArg
                  + ") { this."
                  + methodName
                  + "("
                  + invokeArgs
                  + ") } }"
              : "("
                  + lambdaArgs
                  + ") -> invoker.invoke("
                  + operationArg
                  + ", () -> this."
                  + methodName
                  + "("
                  + invokeArgs
                  + "))";

      String prefix = kt ? "" : "new ";
      String serverMethod = "io.modelcontextprotocol.server." + featuresClass + ".";

      if (route.isMcpTool()) {
        buffer.append(
            statement(
                indent(6),
                "server.addTool(",
                prefix,
                serverMethod,
                "SyncToolSpecification(",
                methodName,
                "ToolSpec(schemaGenerator), ",
                lambda,
                "))",
                semicolon(kt)));
      } else if (route.isMcpPrompt()) {
        buffer.append(
            statement(
                indent(6),
                "server.addPrompt(",
                prefix,
                serverMethod,
                "SyncPromptSpecification(",
                methodName,
                "PromptSpec(), ",
                lambda,
                "))",
                semicolon(kt)));
      } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
        var isTemplate = route.isMcpResourceTemplate();
        var specType =
            isTemplate ? "SyncResourceTemplateSpecification" : "SyncResourceSpecification";
        var addMethod = isTemplate ? "server.addResourceTemplate(" : "server.addResource(";
        var defMethod = isTemplate ? "ResourceTemplateSpec()" : "ResourceSpec()";

        buffer.append(
            statement(
                indent(6),
                addMethod,
                prefix,
                serverMethod,
                specType,
                "(",
                methodName,
                defMethod,
                ", ",
                lambda,
                "))",
                semicolon(kt)));
      }
    }
    buffer.append(statement(indent(4), "}\n"));
  }

  private void appendCompletionHandlers(
      StringBuilder buffer, boolean kt, Map<String, List<McpRoute>> completionGroups) {
    for (var entry : completionGroups.entrySet()) {
      var ref = entry.getKey();
      var handlerName = findTargetMethodName(ref) + "CompletionHandler";

      if (kt) {
        buffer.append(
            statement(
                indent(4),
                "private fun ",
                handlerName,
                "(exchange: io.modelcontextprotocol.server.McpSyncServerExchange?,"
                    + " transportContext: io.modelcontextprotocol.common.McpTransportContext, req:"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteRequest):"
                    + " io.modelcontextprotocol.spec.McpSchema.CompleteResult {"));
        buffer.append(
            statement(indent(6), "val ctx = transportContext.get(\"CTX\") as io.jooby.Context"));
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
                "var ctx = (io.jooby.Context) transportContext.get(\"CTX\")",
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

      for (var route : entry.getValue()) {
        String targetArgName = null;
        var invokeArgs = new ArrayList<String>();

        for (var param : route.getParameters(true)) {
          var type = param.getType().getRawType().toString();
          if (type.equals("io.jooby.Context")) invokeArgs.add("ctx");
          else if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange"))
            invokeArgs.add("exchange");
          else if (type.equals("io.modelcontextprotocol.common.McpTransportContext"))
            invokeArgs.add("transportContext");
          else {
            targetArgName = param.getMcpName();
            invokeArgs.add("typedValue");
          }
        }

        if (targetArgName != null) {
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
                statement(
                    indent(10), "io.jooby.mcp.McpResult(this.json).toCompleteResult(result)"));
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
  }

  private String generateOperationArg(boolean kt, String operationId, String targetMethod) {
    String prefix = kt ? "" : "new ";
    return prefix
        + "io.jooby.mcp.McpOperation("
        + string(operationId)
        + ", "
        + string(getTargetType().toString())
        + ", "
        + string(targetMethod)
        + ")";
  }

  public Optional<MethodDoc> getMethodDoc(String methodName, List<String> types) {
    return javadoc.parse(getTargetType().toString()).flatMap(it -> it.getMethod(methodName, types));
  }
}
