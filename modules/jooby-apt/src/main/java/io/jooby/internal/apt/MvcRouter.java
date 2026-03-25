/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.VALUE;
import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static io.jooby.internal.apt.CodeBlock.*;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.*;

public class MvcRouter {
  public static final String JAVA =
      """
      package ${packageName};
      ${imports}
      @io.jooby.annotation.Generated(${className}.class)
      public class ${generatedClassName} implements ${implements} {
          protected java.util.function.Function<io.jooby.Context, ${className}> factory;
      ${constructors}
          public ${generatedClassName}(${className} instance) {
             setup(ctx -> instance);
          }

          public ${generatedClassName}(io.jooby.SneakyThrows.Supplier<${className}> provider) {
             setup(ctx -> (${className}) provider.get());
          }

          public ${generatedClassName}(io.jooby.SneakyThrows.Function<Class<${className}>, ${className}> provider) {
             setup(ctx -> provider.apply(${className}.class));
          }

          private void setup(java.util.function.Function<io.jooby.Context, ${className}> factory) {
              this.factory = factory;
          }

      ${methods}
      }

      """;
  public static final String KOTLIN =
      """
      package ${packageName}
          ${imports}
          @io.jooby.annotation.Generated(${className}::class)
          open class ${generatedClassName} : ${implements} {
              private lateinit var factory: java.util.function.Function<io.jooby.Context, ${className}>

              ${constructors}
              constructor(instance: ${className}) { setup { instance } }

              constructor(provider: io.jooby.SneakyThrows.Supplier<${className}>) { setup { provider.get() } }

              constructor(provider: (Class<${className}>) -> ${className}) { setup { provider(${className}::class.java) } }

              constructor(provider:  io.jooby.SneakyThrows.Function<Class<${className}>, ${className}>) { setup { provider.apply(${className}::class.java) } }

              private fun setup(factory: java.util.function.Function<io.jooby.Context, ${className}>) {
                this.factory = factory
              }
          ${methods}
          }

      """;

  private final MvcContext context;

  /** MVC router class. */
  private final TypeElement clazz;

  /** MVC route methods. */
  private final Map<String, MvcRoute> routes = new LinkedHashMap<>();

  public MvcRouter(MvcContext context, TypeElement clazz) {
    this.context = context;
    this.clazz = clazz;

    // JSON-RPC Method Discovery Logic
    var classJsonRpcAnno =
        AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.JsonRpc");

    List<ExecutableElement> explicitlyAnnotated = new ArrayList<>();
    List<ExecutableElement> allPublicMethods = new ArrayList<>();

    for (var enclosed : clazz.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.METHOD) {
        var method = (ExecutableElement) enclosed;
        var modifiers = method.getModifiers();

        // Only consider public, non-static, non-abstract methods
        if (modifiers.contains(Modifier.PUBLIC)
            && !modifiers.contains(Modifier.STATIC)
            && !modifiers.contains(Modifier.ABSTRACT)) {

          // Ignore standard Java Object methods
          String methodName = method.getSimpleName().toString();
          if (methodName.equals("toString")
              || methodName.equals("hashCode")
              || methodName.equals("equals")
              || methodName.equals("clone")) {
            continue;
          }

          allPublicMethods.add(method);
          var methodJsonRpcAnno =
              AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.JsonRpc");
          if (methodJsonRpcAnno != null) {
            explicitlyAnnotated.add(method);
          }
        }
      }
    }

    if (!explicitlyAnnotated.isEmpty()) {
      // Rule 2: If one or more methods are explicitly annotated, ONLY expose those methods.
      for (var method : explicitlyAnnotated) {
        var methodAnno =
            AnnotationSupport.findAnnotationByName(method, "io.jooby.annotation.JsonRpc");
        TypeElement annoElement = (TypeElement) methodAnno.getAnnotationType().asElement();
        put(annoElement, method);
      }
    } else if (classJsonRpcAnno != null) {
      // Rule 1: Class is annotated, but no specific methods are. Expose ALL public methods.
      var annoElement = (TypeElement) classJsonRpcAnno.getAnnotationType().asElement();
      for (var method : allPublicMethods) {
        put(annoElement, method);
      }
    }
  }

  public MvcRouter(TypeElement clazz, MvcRouter parent) {
    this.context = parent.context;
    this.clazz = clazz;
    for (var e : parent.routes.entrySet()) {
      this.routes.put(e.getKey(), new MvcRoute(context, this, e.getValue()));
    }
  }

  public boolean isKt() {
    return context
        .getProcessingEnvironment()
        .getElementUtils()
        .getAllAnnotationMirrors(getTargetType())
        .stream()
        .anyMatch(it -> it.getAnnotationType().asElement().toString().equals("kotlin.Metadata"));
  }

  public TypeElement getTargetType() {
    return clazz;
  }

  public String getGeneratedType() {
    String baseName = getTargetType().getQualifiedName().toString();
    String name = isJsonRpc() ? baseName + "Rpc" : baseName;
    return context.generateRouterName(name);
  }

  public MvcRouter put(TypeElement httpMethod, ExecutableElement route) {
    var isTrpc =
        HttpMethod.findByAnnotationName(httpMethod.getQualifiedName().toString())
            == HttpMethod.tRPC;
    var isJsonRpc =
        HttpMethod.findByAnnotationName(httpMethod.getQualifiedName().toString())
            == HttpMethod.JSON_RPC;

    var routeKey = (isTrpc ? "trpc" : (isJsonRpc ? "jsonrpc" : "")) + route.toString();
    var existing = routes.get(routeKey);

    if (existing == null) {
      routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
    } else {
      if (existing.getMethod().getEnclosingElement().equals(getTargetType())) {
        existing.addHttpMethod(httpMethod);
      } else {
        // Favor override version of same method
        routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
      }
    }
    return this;
  }

  public List<MvcRoute> getRoutes() {
    return routes.values().stream().toList();
  }

  public boolean isAbstract() {
    return clazz.getModifiers().contains(Modifier.ABSTRACT);
  }

  public String getPackageName() {
    var classname = getGeneratedType();
    var pkgEnd = classname.lastIndexOf('.');
    return pkgEnd > 0 ? classname.substring(0, pkgEnd) : "";
  }

  public boolean isJsonRpc() {
    return getRoutes().stream().anyMatch(MvcRoute::isJsonRpc);
  }

  public String getJsonRpcNamespace() {
    var annotation = AnnotationSupport.findAnnotationByName(clazz, "io.jooby.annotation.JsonRpc");
    if (annotation != null) {
      return AnnotationSupport.findAnnotationValue(annotation, VALUE).stream()
          .findFirst()
          .orElse("");
    }
    return "";
  }

  public boolean hasRestRoutes() {
    return getRoutes().stream().anyMatch(it -> !it.isJsonRpc() && !it.isMcpRoute());
  }

  public boolean hasJsonRpcRoutes() {
    return getRoutes().stream().anyMatch(MvcRoute::isJsonRpc);
  }

  public String getRestGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString());
  }

  public String getRpcGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString() + "Rpc");
  }

  public String getRestGeneratedFilename() {
    return getRestGeneratedType().replace('.', '/') + (isKt() ? ".kt" : ".java");
  }

  public String getRpcGeneratedFilename() {
    return getRpcGeneratedType().replace('.', '/') + (isKt() ? ".kt" : ".java");
  }

  /**
   * Generate the controller extension for MVC controller:
   *
   * <pre>{@code
   * public class Controller_ implements MvcExtension {
   * ....
   * }
   *
   * }</pre>
   *
   * @return The source code to write, or null if the controller only contains JSON-RPC routes.
   */
  public String getRestSourceCode(Boolean generateKotlin) throws IOException {
    var mvcRoutes =
        this.routes.values().stream().filter(it -> !it.isJsonRpc() && !it.isMcpRoute()).toList();

    if (mvcRoutes.isEmpty()) {
      return null; // Safety check if called on a JSON-RPC-only controller
    }

    var kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = context.generateRouterName(generateTypeName);

    var template = kt ? KOTLIN : JAVA;
    var suspended = mvcRoutes.stream().filter(MvcRoute::isSuspendFun).toList();
    var noSuspended = mvcRoutes.stream().filter(it -> !it.isSuspendFun()).toList();
    var buffer = new StringBuilder();
    context.generateStaticImports(
        null,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    // begin install
    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(app: io.jooby.Jooby) {")
          .append(System.lineSeparator());
    } else {
      buffer
          .append(indent(4))
          .append("public void install(io.jooby.Jooby app) throws Exception {")
          .append(System.lineSeparator());
    }
    if (!suspended.isEmpty()) {
      buffer.append(statement(indent(6), "val kooby = app as io.jooby.kt.Kooby"));
      buffer.append(statement(indent(6), "kooby.coroutine {"));
      suspended.stream()
          .flatMap(it -> it.generateMapping(kt).stream())
          .forEach(line -> buffer.append(CodeBlock.indent(8)).append(line));
      trimr(buffer);
      buffer.append(System.lineSeparator()).append(statement(indent(6), "}"));
    }
    noSuspended.stream()
        .flatMap(it -> it.generateMapping(kt).stream())
        .forEach(line -> buffer.append(CodeBlock.indent(6)).append(line));
    trimr(buffer);
    buffer
        .append(System.lineSeparator())
        .append(indent(4))
        .append("}")
        .append(System.lineSeparator())
        .append(System.lineSeparator());
    // end install

    mvcRoutes.stream()
        .flatMap(it -> it.generateHandlerCall(kt).stream())
        .forEach(line -> buffer.append(CodeBlock.indent(4)).append(line));

    return template
        .replace("${packageName}", getPackageName())
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", generatedClass)
        .replace("${implements}", "io.jooby.Extension")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }

  public String getRpcSourceCode(Boolean generateKotlin) {
    var rpcRoutes = getRoutes().stream().filter(MvcRoute::isJsonRpc).toList();
    if (rpcRoutes.isEmpty()) {
      return null;
    }

    var kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = context.generateRouterName(generateTypeName + "Rpc");
    var namespace = getJsonRpcNamespace();

    var template = kt ? KOTLIN : JAVA;
    var buffer = new StringBuilder();

    context.generateStaticImports(
        null,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    List<String> fullMethods = new ArrayList<>();
    for (MvcRoute route : rpcRoutes) {
      String routeName = route.getJsonRpcMethodName();
      fullMethods.add(namespace.isEmpty() ? routeName : namespace + "." + routeName);
    }

    String methodListString =
        fullMethods.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));

    if (kt) {
      buffer.append(indent(4)).append("@Throws(Exception::class)").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("override fun install(app: io.jooby.Jooby) {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("app.services.listOf(io.jooby.rpc.jsonrpc.JsonRpcService::class.java).add(this)")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer
          .append(indent(4))
          .append("override fun getMethods(): List<String> {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("return listOf(")
          .append(methodListString)
          .append(")")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer
          .append(indent(4))
          .append(
              "override fun execute(ctx: io.jooby.Context, req:"
                  + " io.jooby.rpc.jsonrpc.JsonRpcRequest): Any? {")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("val c = factory.apply(ctx)").append(System.lineSeparator());
      buffer.append(indent(6)).append("val method = req.method").append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("val parser = ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser::class.java)")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("return when(method) {").append(System.lineSeparator());

      for (int i = 0; i < rpcRoutes.size(); i++) {
        buffer
            .append(indent(8))
            .append("\"")
            .append(fullMethods.get(i))
            .append("\" -> {")
            .append(System.lineSeparator());
        rpcRoutes.get(i).generateJsonRpcDispatchCase(true).forEach(buffer::append);
        buffer.append(indent(8)).append("}").append(System.lineSeparator());
      }

      buffer
          .append(indent(8))
          .append(
              "else -> throw"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: $method\")")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("}").append(System.lineSeparator());
      buffer.append(indent(4)).append("}").append(System.lineSeparator());

    } else {
      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("public void install(io.jooby.Jooby app) throws Exception {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("app.getServices().listOf(io.jooby.rpc.jsonrpc.JsonRpcService.class).add(this);")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("public java.util.List<String> getMethods() {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("return java.util.List.of(")
          .append(methodListString)
          .append(");")
          .append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append("}")
          .append(System.lineSeparator())
          .append(System.lineSeparator());

      buffer.append(indent(4)).append("@Override").append(System.lineSeparator());
      buffer
          .append(indent(4))
          .append(
              "public Object execute(io.jooby.Context ctx, io.jooby.rpc.jsonrpc.JsonRpcRequest req)"
                  + " throws Exception {")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append(generateTypeName)
          .append(" c = factory.apply(ctx);")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append("String method = req.getMethod();")
          .append(System.lineSeparator());
      buffer
          .append(indent(6))
          .append(
              "io.jooby.rpc.jsonrpc.JsonRpcParser parser ="
                  + " ctx.require(io.jooby.rpc.jsonrpc.JsonRpcParser.class);")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("switch(method) {").append(System.lineSeparator());

      for (int i = 0; i < rpcRoutes.size(); i++) {
        buffer
            .append(indent(8))
            .append("case \"")
            .append(fullMethods.get(i))
            .append("\": {")
            .append(System.lineSeparator());
        rpcRoutes.get(i).generateJsonRpcDispatchCase(false).forEach(buffer::append);
        buffer.append(indent(8)).append("}").append(System.lineSeparator());
      }

      buffer.append(indent(8)).append("default:").append(System.lineSeparator());
      buffer
          .append(indent(10))
          .append(
              "throw new"
                  + " io.jooby.rpc.jsonrpc.JsonRpcException(io.jooby.rpc.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: \" + method);")
          .append(System.lineSeparator());
      buffer.append(indent(6)).append("}").append(System.lineSeparator());
      buffer.append(indent(4)).append("}").append(System.lineSeparator());
    }

    return template
        .replace("${packageName}", getPackageName())
        .replace("${imports}", imports)
        .replace("${className}", generateTypeName)
        .replace("${generatedClassName}", generatedClass)
        .replace("${implements}", "io.jooby.rpc.jsonrpc.JsonRpcService, io.jooby.Extension")
        .replace("${constructors}", constructors(generatedClass, kt))
        .replace("${methods}", trimr(buffer));
  }

  public boolean hasMcpRoutes() {
    return getRoutes().stream().anyMatch(MvcRoute::isMcpRoute);
  }

  public String getMcpGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString() + "Mcp");
  }

  public String getMcpGeneratedFilename() {
    return getMcpGeneratedType().replace('.', '/') + (isKt() ? ".kt" : ".java");
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

  public String getMcpSourceCode(Boolean generateKotlin) {
    if (!hasMcpRoutes()) {
      return null;
    }

    boolean kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var mcpClassName = context.generateRouterName(generateTypeName + "Mcp");
    var packageName = getPackageName();

    var template = kt ? KOTLIN : JAVA;
    var buffer = new StringBuilder();

    context.generateStaticImports(
        null,
        (owner, fn) ->
            buffer.append(
                statement("import ", kt ? "" : "static ", owner, ".", fn, semicolon(kt))));
    var imports = buffer.toString();
    buffer.setLength(0);

    var tools = getRoutes().stream().filter(MvcRoute::isMcpTool).toList();
    var prompts = getRoutes().stream().filter(MvcRoute::isMcpPrompt).toList();
    // FIXED: Now properly includes Templates so capabilities.resources() activates
    var resources =
        getRoutes().stream().filter(r -> r.isMcpResource() || r.isMcpResourceTemplate()).toList();

    // 1. Group Completions by Reference
    var completionRoutes = getRoutes().stream().filter(MvcRoute::isMcpCompletion).toList();
    java.util.Map<String, java.util.List<MvcRoute>> completionGroups =
        new java.util.LinkedHashMap<>();
    for (MvcRoute route : completionRoutes) {
      String ref = extractAnnotationValue(route, "io.jooby.annotation.McpCompletion", "value");
      if (ref == null || ref.isEmpty()) {
        ref = extractAnnotationValue(route, "io.jooby.annotation.McpCompletion", "ref");
      }
      completionGroups.computeIfAbsent(ref, k -> new java.util.ArrayList<>()).add(route);
    }

    // Generate JSON Mapper Field
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

    // Generate capabilities()
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

    // Generate serverName()
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

    // Generate completions() list
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

    // Generate install()
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
                    + " com.github.victools.jsonschema.generator.SchemaGenerator(configBuilder.build())"));
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
                semicolon(kt)));
      }
    }

    // FIXED: Filter now properly includes isMcpResourceTemplate()
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
        // FIXED: Condition now allows templates to execute this block!
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

    // FIXED: Filter now properly includes isMcpResourceTemplate()
    for (MvcRoute route :
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

    // --- STEP 3: GENERATE THE UNIFIED COMPLETION HANDLERS (THE ROUTER) ---
    for (var entry : completionGroups.entrySet()) {
      String ref = entry.getKey();
      String handlerName = findTargetMethodName(ref) + "CompletionHandler";
      java.util.List<MvcRoute> routes = entry.getValue();

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

      for (MvcRoute route : routes) {
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

      // Default fallback returning the empty list
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
        buffer.append(
            statement(
                indent(6),
                "}",
                semicolon(kt))); // Note: The semicolon here closes the return statement!
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

  private String findTargetMethodName(String ref) {
    for (MvcRoute route : getRoutes()) {
      if (route.isMcpPrompt()) {
        String name = extractAnnotationValue(route, "io.jooby.annotation.McpPrompt", "name");
        if (name == null || name.isEmpty()) name = route.getMethodName();
        if (ref.equals(name)) return route.getMethodName();
      } else if (route.isMcpResource() || route.isMcpResourceTemplate()) {
        // Now checks BOTH route types, but only reads from @McpResource
        String uri = extractAnnotationValue(route, "io.jooby.annotation.McpResource", "value");
        if (ref.equals(uri)) return route.getMethodName();
      }
    }
    return "mcpTarget" + Math.abs(ref.hashCode());
  }

  private StringBuilder trimr(StringBuilder buffer) {
    var i = buffer.length() - 1;
    while (i > 0 && Character.isWhitespace(buffer.charAt(i))) {
      buffer.deleteCharAt(i);
      i = buffer.length() - 1;
    }
    return buffer;
  }

  private String extractAnnotationValue(MvcRoute route, String annotationName, String attribute) {
    var annotation =
        io.jooby.internal.apt.AnnotationSupport.findAnnotationByName(
            route.getMethod(), annotationName);
    if (annotation == null) {
      return "";
    }
    return io.jooby.internal.apt.AnnotationSupport.findAnnotationValue(
            annotation, attribute::equals)
        .stream()
        .findFirst()
        .orElse("");
  }

  private StringBuilder constructors(String generatedName, boolean kt) {
    var constructors =
        getTargetType().getEnclosedElements().stream()
            .filter(
                it ->
                    it.getKind() == ElementKind.CONSTRUCTOR
                        && it.getModifiers().contains(Modifier.PUBLIC))
            .map(ExecutableElement.class::cast)
            .toList();
    var targetType = getTargetType().getSimpleName();
    var buffer = new StringBuilder();
    buffer.append(System.lineSeparator());
    // Inject could be at constructor or field level.
    var injectConstructor =
        constructors.stream().filter(hasInjectAnnotation()).findFirst().orElse(null);
    var inject = injectConstructor != null || hasInjectAnnotation(getTargetType());
    final var defaultConstructor =
        constructors.stream().filter(it -> it.getParameters().isEmpty()).findFirst().orElse(null);
    if (inject) {
      constructor(
          generatedName,
          kt,
          kt ? ":" : null,
          buffer,
          List.of(),
          (output, params) -> {
            output
                .append("this(")
                .append(targetType)
                .append(kt ? "::class" : ".class")
                .append(")")
                .append(semicolon(kt))
                .append(System.lineSeparator());
          });
    } else {
      if (defaultConstructor != null) {
        constructor(
            generatedName,
            kt,
            kt ? ":" : null,
            buffer,
            List.of(),
            (output, params) -> {
              if (kt) {
                output
                    .append("this(")
                    .append(targetType)
                    .append("())")
                    .append(semicolon(true))
                    .append(System.lineSeparator());
              } else {
                output
                    .append("this(")
                    .append("io.jooby.SneakyThrows.singleton(")
                    .append(targetType)
                    .append("::new))")
                    .append(semicolon(false))
                    .append(System.lineSeparator());
              }
            });
      }
    }
    var skip =
        Stream.of(injectConstructor, defaultConstructor)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (ExecutableElement constructor : constructors) {
      if (!skip.contains(constructor)) {
        constructor(
            generatedName,
            kt,
            kt ? ":" : null,
            buffer,
            constructor.getParameters().stream()
                .map(it -> Map.<Object, String>entry(it.asType(), it.getSimpleName().toString()))
                .toList(),
            (output, params) -> {
              var separator = ", ";
              output.append("this(").append(kt ? "" : "new ").append(targetType).append("(");
              params.forEach(e -> output.append(e.getValue()).append(separator));
              output.setLength(output.length() - separator.length());
              output.append("))").append(semicolon(kt)).append(System.lineSeparator());
            });
      }
    }

    if (inject) {
      if (kt) {
        constructor(
            generatedName,
            true,
            "{",
            buffer,
            List.of(Map.entry("kotlin.reflect.KClass<" + targetType + ">", "type")),
            (output, params) -> {
              output
                  .append("setup { ctx -> ctx.require<")
                  .append(targetType)
                  .append(">(type.java)")
                  .append(" }")
                  .append(System.lineSeparator());
            });
      } else {
        constructor(
            generatedName,
            false,
            null,
            buffer,
            List.of(Map.entry("Class<" + targetType + ">", "type")),
            (output, params) -> {
              output
                  .append("setup(")
                  .append("ctx -> ctx.require(type)")
                  .append(")")
                  .append(";")
                  .append(System.lineSeparator());
            });
      }
    }

    return trimr(buffer).append(System.lineSeparator());
  }

  private boolean hasInjectAnnotation(TypeElement targetClass) {
    var inject = false;
    while (!inject && !targetClass.toString().equals("java.lang.Object")) {
      // Look up at field/setter injection
      inject = targetClass.getEnclosedElements().stream().anyMatch(hasInjectAnnotation());
      targetClass =
          (TypeElement)
              context
                  .getProcessingEnvironment()
                  .getTypeUtils()
                  .asElement(targetClass.getSuperclass());
    }
    return inject;
  }

  private static Predicate<Element> hasInjectAnnotation() {
    var injectAnnotations =
        Set.of("javax.inject.Inject", "jakarta.inject.Inject", "com.google.inject.Inject");
    return it ->
        injectAnnotations.stream()
            .anyMatch(annotation -> findAnnotationByName(it, annotation) != null);
  }

  private void constructor(
      String generatedName,
      boolean kt,
      String ktBody,
      StringBuilder buffer,
      List<Map.Entry<Object, String>> parameters,
      BiConsumer<StringBuilder, List<Map.Entry<Object, String>>> body) {
    buffer.append(indent(4));
    if (kt) {
      buffer.append("constructor").append("(");
    } else {
      buffer.append("public ").append(generatedName).append("(");
    }
    var separator = ", ";
    parameters.forEach(
        e -> {
          if (kt) {
            buffer.append(e.getValue()).append(": ").append(e.getKey()).append(separator);
          } else {
            buffer.append(e.getKey()).append(" ").append(e.getValue()).append(separator);
          }
        });
    if (!parameters.isEmpty()) {
      buffer.setLength(buffer.length() - separator.length());
    }
    buffer.append(")");
    if (!kt) {
      buffer.append(" {").append(System.lineSeparator());
      buffer.append(indent(6));
    } else {
      buffer.append(" ").append(ktBody).append(" ");
    }
    body.accept(buffer, parameters);
    if (!kt || "{".equals(ktBody)) {
      buffer.append(indent(4)).append("}");
    }
    buffer.append(System.lineSeparator()).append(System.lineSeparator());
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    var annotations = Optional.ofNullable(clazz.getAnnotationMirrors()).orElse(emptyList());
    annotations.forEach(
        annotation -> {
          buffer
              .append("@")
              .append(annotation.getAnnotationType().asElement().getSimpleName())
              .append("(");
          buffer.append(annotation.getElementValues()).append(") ");
        });
    buffer.append(clazz.asType().toString()).append(" {\n");
    routes.forEach(
        (httpMethod, route) -> {
          buffer.append("  ").append(route).append("\n");
        });
    buffer.append("}");
    return buffer.toString();
  }

  public boolean hasBeanValidation() {
    return getRoutes().stream().anyMatch(MvcRoute::hasBeanValidation);
  }
}
