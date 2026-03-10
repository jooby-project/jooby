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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.*;

public class MvcRouter {
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

  public String getGeneratedFilename() {
    return getGeneratedType().replace('.', '/') + (isKt() ? ".kt" : ".java");
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
    return getRoutes().stream().anyMatch(it -> !it.isJsonRpc());
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
    var mvcRoutes = this.routes.values().stream().filter(it -> !it.isJsonRpc()).toList();

    if (mvcRoutes.isEmpty()) {
      return null; // Safety check if called on a JSON-RPC-only controller
    }

    var kt = generateKotlin == Boolean.TRUE || isKt();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var generatedClass = context.generateRouterName(generateTypeName);

    try (var in = getClass().getResourceAsStream("Source" + (kt ? ".kt" : ".java"))) {
      Objects.requireNonNull(in);
      var suspended = mvcRoutes.stream().filter(MvcRoute::isSuspendFun).toList();
      var noSuspended = mvcRoutes.stream().filter(it -> !it.isSuspendFun()).toList();
      var buffer = new StringBuilder();
      context.generateStaticImports(
          this,
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

      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replace("${packageName}", getPackageName())
          .replace("${imports}", imports)
          .replace("${className}", generateTypeName)
          .replace("${generatedClassName}", generatedClass)
          .replace("${constructors}", constructors(generatedClass, kt))
          .replace("${methods}", trimr(buffer));
    }
  }

  public String getRpcSourceCode(Boolean generateKotlin) {
    if (!hasJsonRpcRoutes()) {
      return null;
    }
    return generateJsonRpcService(generateKotlin == Boolean.TRUE || isKt());
  }

  private String generateJsonRpcService(boolean kt) {
    var buffer = new StringBuilder();
    var generateTypeName = getTargetType().getSimpleName().toString();
    var rpcClassName = context.generateRouterName(generateTypeName + "Rpc");
    var namespace = getJsonRpcNamespace();
    var packageName = getPackageName();

    var rpcRoutes = getRoutes().stream().filter(MvcRoute::isJsonRpc).toList();

    List<String> fullMethods = new ArrayList<>();
    for (MvcRoute route : rpcRoutes) {
      String routeName = route.getJsonRpcMethodName();
      fullMethods.add(namespace.isEmpty() ? routeName : namespace + "." + routeName);
    }

    String methodListString =
        fullMethods.stream().map(m -> "\"" + m + "\"").collect(Collectors.joining(", "));

    buffer.append(statement("package ", packageName, semicolon(kt)));
    buffer.append(System.lineSeparator());

    buffer.append(
        statement(
            "@io.jooby.annotation.Generated(", generateTypeName, kt ? "::class" : ".class", ")"));

    if (kt) {
      buffer.append(
          statement(
              "class ", rpcClassName, " : io.jooby.jsonrpc.JsonRpcService, io.jooby.Extension {"));
      buffer.append(
          statement(
              indent(2),
              "protected lateinit var factory: (io.jooby.Context) -> ",
              generateTypeName));

      String ktConstructors = constructors(rpcClassName, true).toString().replaceAll("(?m)^  ", "");
      buffer.append(ktConstructors);
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "constructor(instance: ", generateTypeName, ") {"));
      buffer.append(statement(indent(4), "setup { ctx -> instance }"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "constructor(provider: io.jooby.SneakyThrows.Supplier<",
              generateTypeName,
              ">) {"));
      buffer.append(statement(indent(4), "setup { ctx -> provider.get() }"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "constructor(provider: io.jooby.SneakyThrows.Function<Class<",
              generateTypeName,
              ">, ",
              generateTypeName,
              ">) {"));
      buffer.append(
          statement(
              indent(4), "setup { ctx -> provider.apply(", generateTypeName, "::class.java) }"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "private fun setup(factory: (io.jooby.Context) -> ",
              generateTypeName,
              ") {"));
      buffer.append(statement(indent(4), "this.factory = factory"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "override fun install(app: io.jooby.Jooby) {"));
      buffer.append(
          statement(
              indent(4),
              "app.services.listOf(io.jooby.jsonrpc.JsonRpcService::class.java).add(this)"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "override fun getMethods(): List<String> {"));
      buffer.append(statement(indent(4), "return listOf(", methodListString, ")"));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "@Throws(Exception::class)"));
      buffer.append(
          statement(
              indent(2),
              "override fun execute(ctx: io.jooby.Context, req: io.jooby.jsonrpc.JsonRpcRequest):"
                  + " Any? {"));
      buffer.append(statement(indent(4), "val c = factory(ctx)"));
      buffer.append(statement(indent(4), "val method = req.method"));
      buffer.append(
          statement(
              indent(4),
              var(kt),
              "parser = ctx.require(io.jooby.jsonrpc.JsonRpcParser",
              clazz(kt),
              ")",
              semicolon(kt)));
      buffer.append(statement(indent(4), "return when(method) {"));

      for (int i = 0; i < rpcRoutes.size(); i++) {
        buffer.append(statement(indent(6), "\"", fullMethods.get(i), "\" -> {"));
        rpcRoutes.get(i).generateJsonRpcDispatchCase(true).forEach(buffer::append);
        buffer.append(statement(indent(6), "}"));
      }

      buffer.append(
          statement(
              indent(6),
              "else -> throw"
                  + " io.jooby.jsonrpc.JsonRpcException(io.jooby.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: $method\")"));
      buffer.append(statement(indent(4), "}"));
      buffer.append(statement(indent(2), "}"));

    } else {
      buffer.append(
          statement(
              "public class ",
              rpcClassName,
              " implements io.jooby.jsonrpc.JsonRpcService, io.jooby.Extension {"));
      buffer.append(
          statement(
              indent(2),
              "protected java.util.function.Function<io.jooby.Context, ",
              generateTypeName,
              "> factory",
              semicolon(kt)));

      String javaConstructors =
          constructors(rpcClassName, false).toString().replaceAll("(?m)^  ", "");
      buffer.append(javaConstructors);
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(indent(2), "public ", rpcClassName, "(", generateTypeName, " instance) {"));
      buffer.append(statement(indent(4), "setup(ctx -> instance)", semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "public ",
              rpcClassName,
              "(io.jooby.SneakyThrows.Supplier<",
              generateTypeName,
              "> provider) {"));
      buffer.append(
          statement(
              indent(4), "setup(ctx -> (", generateTypeName, ") provider.get())", semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "public ",
              rpcClassName,
              "(io.jooby.SneakyThrows.Function<Class<",
              generateTypeName,
              ">, ",
              generateTypeName,
              "> provider) {"));
      buffer.append(
          statement(
              indent(4),
              "setup(ctx -> provider.apply(",
              generateTypeName,
              ".class))",
              semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(
          statement(
              indent(2),
              "private void setup(java.util.function.Function<io.jooby.Context, ",
              generateTypeName,
              "> factory) {"));
      buffer.append(statement(indent(4), "this.factory = factory", semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "@Override"));
      buffer.append(
          statement(indent(2), "public void install(io.jooby.Jooby app) throws Exception {"));
      buffer.append(
          statement(
              indent(4),
              "app.getServices().listOf(io.jooby.jsonrpc.JsonRpcService.class).add(this)",
              semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "@Override"));
      buffer.append(statement(indent(2), "public java.util.List<String> getMethods() {"));
      buffer.append(
          statement(indent(4), "return java.util.List.of(", methodListString, ")", semicolon(kt)));
      buffer.append(statement(indent(2), "}"));
      buffer.append(System.lineSeparator());

      buffer.append(statement(indent(2), "@Override"));
      buffer.append(
          statement(
              indent(2),
              "public Object execute(io.jooby.Context ctx, io.jooby.jsonrpc.JsonRpcRequest req)"
                  + " throws Exception {"));
      buffer.append(
          statement(indent(4), generateTypeName, " c = factory.apply(ctx)", semicolon(kt)));
      buffer.append(statement(indent(4), "String method = req.getMethod()", semicolon(kt)));
      buffer.append(
          statement(
              indent(4),
              var(kt),
              "parser = ctx.require(io.jooby.jsonrpc.JsonRpcParser",
              clazz(kt),
              ")",
              semicolon(kt)));
      buffer.append(statement(indent(4), "switch(method) {"));

      for (int i = 0; i < rpcRoutes.size(); i++) {
        buffer.append(statement(indent(6), "case \"", fullMethods.get(i), "\": {"));
        rpcRoutes.get(i).generateJsonRpcDispatchCase(false).forEach(buffer::append);
        buffer.append(statement(indent(6), "}"));
      }

      buffer.append(
          statement(
              indent(6),
              "default: throw new"
                  + " io.jooby.jsonrpc.JsonRpcException(io.jooby.jsonrpc.JsonRpcErrorCode.METHOD_NOT_FOUND,"
                  + " \"Method not found: \" + method)",
              semicolon(kt)));
      buffer.append(statement(indent(4), "}"));
      buffer.append(statement(indent(2), "}"));
    }

    buffer.append(statement("}"));

    return buffer.toString();
  }

  private StringBuilder trimr(StringBuilder buffer) {
    var i = buffer.length() - 1;
    while (i > 0 && Character.isWhitespace(buffer.charAt(i))) {
      buffer.deleteCharAt(i);
      i = buffer.length() - 1;
    }
    return buffer;
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
